package vpncore

import (
	"errors"
	"fmt"
	"strconv"
	"syscall"

	tuncore "github.com/xjasonlyu/tun2socks/v2/core"
	"github.com/xjasonlyu/tun2socks/v2/core/device"
	"github.com/xjasonlyu/tun2socks/v2/core/device/fdbased"
	"github.com/xjasonlyu/tun2socks/v2/dialer"
	"github.com/xjasonlyu/tun2socks/v2/proxy"
	"github.com/xjasonlyu/tun2socks/v2/tunnel"
	"github.com/xjasonlyu/tun2socks/v2/tunnel/statistic"
	"gvisor.dev/gvisor/pkg/tcpip/stack"
)

type tunRunner struct {
	device device.Device
	stack  *stack.Stack
	tunnel *tunnel.Tunnel
}

func startTunRunner(fd int, mtu int, tunnelProxy proxy.Proxy, platform Platform, logf func(string)) (*tunRunner, error) {
	statistic.DefaultManager.ResetStatistic()
	dialer.Reset()
	if platform != nil {
		dialer.RegisterSockOpt(dialer.SocketOptionFunc(func(_, _ string, raw syscall.RawConn) error {
			var protectErr error
			if err := raw.Control(func(socket uintptr) {
				if !platform.Protect(int64(socket)) {
					protectErr = errors.New("platform rejected socket protection")
				}
			}); err != nil {
				return err
			}
			return protectErr
		}))
	}
	dev, err := fdbased.Open(strconv.Itoa(fd), uint32(mtu), 0)
	if err != nil {
		return nil, fmt.Errorf("open TUN fd: %w", err)
	}
	handler := tunnel.New(tunnelProxy, statistic.DefaultManager)
	handler.ProcessAsync()
	netstack, err := tuncore.CreateStack(&tuncore.Config{
		LinkEndpoint:     dev,
		TransportHandler: handler,
	})
	if err != nil {
		handler.Close()
		dev.Close()
		return nil, fmt.Errorf("create tun2socks stack: %w", err)
	}
	logf("tun2socks netstack created")
	return &tunRunner{device: dev, stack: netstack, tunnel: handler}, nil
}

func (t *tunRunner) stop() {
	if t.tunnel != nil {
		t.tunnel.Close()
	}
	if t.device != nil {
		t.device.Close()
	}
	if t.stack != nil {
		t.stack.Close()
		t.stack.Wait()
	}
}
