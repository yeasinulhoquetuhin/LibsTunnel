package vpncore

import (
	"bufio"
	"io"
	"net"
	"strconv"
	"testing"
	"time"
)

func TestInjectorPayloadAndBridge(t *testing.T) {
	backend, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer backend.Close()

	serverDone := make(chan error, 1)
	go func() {
		conn, err := backend.Accept()
		if err != nil {
			serverDone <- err
			return
		}
		defer conn.Close()
		reader := bufio.NewReader(conn)
		for {
			line, err := reader.ReadString('\n')
			if err != nil {
				serverDone <- err
				return
			}
			if line == "\r\n" {
				break
			}
		}
		if _, err := io.WriteString(conn, "HTTP/1.1 200 Connection established\r\n\r\n"); err != nil {
			serverDone <- err
			return
		}
		_, err = io.Copy(conn, reader)
		serverDone <- err
	}()

	host, portText, _ := net.SplitHostPort(backend.Addr().String())
	port, _ := strconv.Atoi(portText)
	inject := newInjector(injectionConfig{
		Enabled:               true,
		Mode:                  modeDirectTarget,
		TargetHost:            host,
		TargetPort:            port,
		Payload:               "CONNECT [host_port] HTTP/1.1[crlf]Host: [host][crlf][crlf]",
		Method:                "CONNECT",
		Protocol:              "HTTP/1.1",
		UserAgent:             "test",
		ResponseMode:          responseHeaders,
		AcceptedStatus:        []int{200},
		ConnectTimeoutMillis:  2000,
		ResponseTimeoutMillis: 2000,
	}, nil)
	address, err := inject.start()
	if err != nil {
		t.Fatal(err)
	}
	defer inject.stop()

	client, err := net.DialTimeout("tcp", address, 2*time.Second)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := client.Write([]byte("hello")); err != nil {
		t.Fatal(err)
	}
	response := make([]byte, 5)
	if _, err := io.ReadFull(client, response); err != nil {
		t.Fatal(err)
	}
	if string(response) != "hello" {
		t.Fatalf("unexpected bridged data: %q", response)
	}
	_ = client.Close()

	select {
	case err := <-serverDone:
		if err != nil {
			t.Fatal(err)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("backend did not finish")
	}
}
