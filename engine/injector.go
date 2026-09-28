package vpncore

import (
	"bufio"
	"context"
	"crypto/tls"
	"errors"
	"fmt"
	"io"
	"net"
	"strconv"
	"strings"
	"sync"
	"syscall"
	"time"
)

type Platform interface {
	Log(message string)
	Protect(socket int64) bool
}

type injector struct {
	cfg      injectionConfig
	platform Platform
	listener net.Listener
	dialHost string
	cancel   context.CancelFunc
	wg       sync.WaitGroup
	activeMu sync.Mutex
	active   map[net.Conn]struct{}
}

func newInjector(cfg injectionConfig, platform Platform) *injector {
	return &injector{cfg: cfg, platform: platform, active: make(map[net.Conn]struct{})}
}

func (i *injector) start() (string, error) {
	connectHost, _ := i.connectEndpoint()
	dialHost, err := resolveOne(connectHost, time.Duration(i.cfg.ConnectTimeoutMillis)*time.Millisecond)
	if err != nil {
		return "", fmt.Errorf("resolve %s: %w", connectHost, err)
	}
	i.dialHost = dialHost

	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		return "", fmt.Errorf("start injector listener: %w", err)
	}
	i.listener = listener
	ctx, cancel := context.WithCancel(context.Background())
	i.cancel = cancel
	i.wg.Add(1)
	go i.acceptLoop(ctx)
	i.log("injector listening on " + listener.Addr().String())
	return listener.Addr().String(), nil
}

func (i *injector) stop() {
	if i.cancel != nil {
		i.cancel()
	}
	if i.listener != nil {
		_ = i.listener.Close()
	}
	i.activeMu.Lock()
	for conn := range i.active {
		_ = conn.Close()
	}
	i.activeMu.Unlock()
	i.wg.Wait()
}

func (i *injector) acceptLoop(ctx context.Context) {
	defer i.wg.Done()
	for {
		conn, err := i.listener.Accept()
		if err != nil {
			select {
			case <-ctx.Done():
				return
			default:
				i.log("injector accept error: " + err.Error())
				continue
			}
		}
		i.track(conn)
		i.wg.Add(1)
		go func() {
			defer i.wg.Done()
			defer i.untrack(conn)
			i.handle(conn)
		}()
	}
}

func (i *injector) handle(client net.Conn) {
	defer client.Close()
	remote, err := i.dialRemote()
	if err != nil {
		i.log("injector dial failed: " + err.Error())
		return
	}
	i.track(remote)
	defer i.untrack(remote)
	defer remote.Close()

	chunks, err := expandPayload(i.cfg.Payload, payloadVariables{
		Host:      i.cfg.TargetHost,
		Port:      i.cfg.TargetPort,
		Method:    i.cfg.Method,
		Protocol:  i.cfg.Protocol,
		UserAgent: i.cfg.UserAgent,
		RealRaw:   net.JoinHostPort(i.cfg.TargetHost, strconv.Itoa(i.cfg.TargetPort)),
	})
	if err != nil {
		i.log("payload error: " + err.Error())
		return
	}
	for _, chunk := range chunks {
		if chunk.delayBefore > 0 {
			time.Sleep(chunk.delayBefore)
		}
		if len(chunk.data) > 0 {
			if _, err := remote.Write(chunk.data); err != nil {
				i.log("payload write failed: " + err.Error())
				return
			}
		}
	}

	var remoteReader io.Reader = remote
	if i.cfg.ResponseMode == responseHeaders {
		reader := bufio.NewReaderSize(remote, 32*1024)
		status, line, err := readResponseHeaders(remote, reader, time.Duration(i.cfg.ResponseTimeoutMillis)*time.Millisecond)
		if err != nil {
			i.log("payload response failed: " + err.Error())
			return
		}
		if !i.cfg.AcceptAnyResponse && !containsStatus(i.cfg.AcceptedStatus, status) {
			i.log(fmt.Sprintf("payload response rejected: %s", line))
			return
		}
		i.log(fmt.Sprintf("payload response accepted: %s", line))
		remoteReader = reader
	}

	bridge(client, remote, remoteReader)
}

func (i *injector) dialRemote() (net.Conn, error) {
	_, port := i.connectEndpoint()
	dialer := &net.Dialer{
		Timeout: time.Duration(i.cfg.ConnectTimeoutMillis) * time.Millisecond,
		Control: func(_, _ string, raw syscall.RawConn) error {
			if i.platform == nil {
				return nil
			}
			var protectErr error
			if err := raw.Control(func(fd uintptr) {
				if !i.platform.Protect(int64(fd)) {
					protectErr = errors.New("platform rejected socket protection")
				}
			}); err != nil {
				return err
			}
			return protectErr
		},
	}
	address := net.JoinHostPort(i.dialHost, strconv.Itoa(port))
	conn, err := dialer.Dial("tcp", address)
	if err != nil {
		return nil, err
	}
	if i.cfg.Mode != modeDirectSNI && i.cfg.Mode != modeProxySNI {
		return conn, nil
	}
	serverName := i.cfg.ServerNameIndication
	if serverName == "" {
		serverName, _ = i.connectEndpoint()
	}
	tlsConn := tls.Client(conn, &tls.Config{
		ServerName:         serverName,
		InsecureSkipVerify: i.cfg.AllowInsecure, // User-controlled for compatibility with private endpoints.
		MinVersion:         tls.VersionTLS12,
	})
	if err := tlsConn.Handshake(); err != nil {
		_ = conn.Close()
		return nil, fmt.Errorf("outer TLS handshake: %w", err)
	}
	return tlsConn, nil
}

func (i *injector) connectEndpoint() (string, int) {
	if i.cfg.Mode == modeProxyTarget || i.cfg.Mode == modeProxySNI {
		return i.cfg.ProxyHost, i.cfg.ProxyPort
	}
	return i.cfg.TargetHost, i.cfg.TargetPort
}

func (i *injector) log(message string) {
	if i.platform != nil {
		i.platform.Log(message)
	}
}

func (i *injector) track(conn net.Conn) {
	i.activeMu.Lock()
	i.active[conn] = struct{}{}
	i.activeMu.Unlock()
}

func (i *injector) untrack(conn net.Conn) {
	i.activeMu.Lock()
	delete(i.active, conn)
	i.activeMu.Unlock()
}

func resolveOne(host string, timeout time.Duration) (string, error) {
	if ip := net.ParseIP(host); ip != nil {
		return ip.String(), nil
	}
	ctx, cancel := context.WithTimeout(context.Background(), timeout)
	defer cancel()
	addresses, err := net.DefaultResolver.LookupIPAddr(ctx, host)
	if err != nil {
		return "", err
	}
	for _, address := range addresses {
		if address.IP.To4() != nil {
			return address.IP.String(), nil
		}
	}
	if len(addresses) > 0 {
		return addresses[0].IP.String(), nil
	}
	return "", errors.New("no IP address returned")
}

func readResponseHeaders(conn net.Conn, reader *bufio.Reader, timeout time.Duration) (int, string, error) {
	if err := conn.SetReadDeadline(time.Now().Add(timeout)); err != nil {
		return 0, "", err
	}
	defer conn.SetReadDeadline(time.Time{})

	line, err := reader.ReadString('\n')
	if err != nil {
		return 0, "", err
	}
	line = strings.TrimSpace(line)
	parts := strings.Fields(line)
	status := 0
	if len(parts) >= 2 {
		status, _ = strconv.Atoi(parts[1])
	}
	read := len(line)
	for {
		header, err := reader.ReadString('\n')
		if err != nil {
			return 0, line, err
		}
		read += len(header)
		if read > 64*1024 {
			return 0, line, errors.New("response headers exceed 64 KiB")
		}
		if header == "\r\n" || header == "\n" {
			return status, line, nil
		}
	}
}

func containsStatus(values []int, target int) bool {
	for _, value := range values {
		if value == target {
			return true
		}
	}
	return false
}

func bridge(client net.Conn, remote net.Conn, remoteReader io.Reader) {
	done := make(chan struct{}, 2)
	go func() {
		_, _ = io.Copy(remote, client)
		if tcp, ok := remote.(*net.TCPConn); ok {
			_ = tcp.CloseWrite()
		}
		done <- struct{}{}
	}()
	go func() {
		_, _ = io.Copy(client, remoteReader)
		if tcp, ok := client.(*net.TCPConn); ok {
			_ = tcp.CloseWrite()
		}
		done <- struct{}{}
	}()
	<-done
}
