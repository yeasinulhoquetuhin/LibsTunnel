package vpncore

import (
	"errors"
	"fmt"
	"net"
	"strconv"
	"sync"
	"time"

	t2proxy "github.com/xjasonlyu/tun2socks/v2/proxy"
	"github.com/xjasonlyu/tun2socks/v2/proxy/socks5"
	sshproxy "github.com/xjasonlyu/tun2socks/v2/proxy/ssh"
	"github.com/xjasonlyu/tun2socks/v2/tunnel/statistic"
	"github.com/xtls/xray-core/core"
)

type Manager struct {
	mu        sync.Mutex
	platform  Platform
	xray      *core.Instance
	injector  *injector
	tun       *tunRunner
	tunConfig tunConfig
	tunProxy  t2proxy.Proxy
}

func NewManager(platform Platform) *Manager {
	return &Manager{platform: platform}
}

func Version() string {
	return "libs-tunnel/1.0 xray/" + core.Version()
}

func (m *Manager) Start(configJSON string) error {
	cfg, err := parseConfig(configJSON)
	if err != nil {
		return err
	}

	m.mu.Lock()
	defer m.mu.Unlock()
	m.stopLocked()

	if cfg.Backend == backendSSH {
		host, err := resolveOne(cfg.SSH.Host, 12*time.Second)
		if err != nil {
			return fmt.Errorf("resolve ssh host: %w", err)
		}
		ssh, err := sshproxy.New(
			net.JoinHostPort(host, strconv.Itoa(cfg.SSH.Port)),
			cfg.SSH.Username,
			cfg.SSH.Password,
			cfg.SSH.PrivateKey,
			cfg.SSH.KeyPassphrase,
		)
		if err != nil {
			return fmt.Errorf("create ssh proxy: %w", err)
		}
		m.tunProxy = ssh
		m.tunConfig = cfg.Tun
		m.log("SSH backend ready")
		return nil
	}

	xrayJSON := []byte(cfg.Xray)
	if cfg.Injection.Enabled {
		// Derive the target before starting the listener, then rewrite Xray to the listener.
		var deriveOnly = cfg.Injection
		if _, err := prepareXrayConfig(cfg.Xray, cfg.OutboundTag, &deriveOnly, "127.0.0.1:1"); err != nil {
			return err
		}
		cfg.Injection.TargetHost = deriveOnly.TargetHost
		cfg.Injection.TargetPort = deriveOnly.TargetPort

		m.injector = newInjector(cfg.Injection, m.platform)
		address, err := m.injector.start()
		if err != nil {
			m.injector = nil
			return err
		}
		xrayJSON, err = prepareXrayConfig(cfg.Xray, cfg.OutboundTag, &cfg.Injection, address)
		if err != nil {
			m.injector.stop()
			m.injector = nil
			return err
		}
	}

	instance, err := core.StartInstance("json", xrayJSON)
	if err != nil {
		if m.injector != nil {
			m.injector.stop()
			m.injector = nil
		}
		return fmt.Errorf("start xray: %w", err)
	}
	m.xray = instance
	socks, err := socks5.New(cfg.Tun.SocksAddress, "", "")
	if err != nil {
		_ = instance.Close()
		m.xray = nil
		return fmt.Errorf("create local SOCKS proxy: %w", err)
	}
	m.tunProxy = socks
	m.tunConfig = cfg.Tun
	m.log("Xray started: " + core.Version())
	return nil
}

func (m *Manager) StartTun(fileDescriptor int64, mtu int64) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	if m.tunProxy == nil {
		return errors.New("start a tunnel backend before starting TUN")
	}
	if m.tun != nil {
		return errors.New("TUN is already running")
	}
	if fileDescriptor < 0 {
		return errors.New("invalid TUN file descriptor")
	}
	if mtu == 0 {
		mtu = int64(m.tunConfig.MTU)
	}
	if mtu < 1280 || mtu > 9000 {
		return errors.New("TUN MTU must be between 1280 and 9000")
	}

	runner, err := startTunRunner(int(fileDescriptor), int(mtu), m.tunProxy, m.platform, m.log)
	if err != nil {
		return err
	}
	m.tun = runner
	m.log(fmt.Sprintf("tun2socks started on fd %d", fileDescriptor))
	return nil
}

func (m *Manager) StopTun() {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.stopTunLocked()
}

func (m *Manager) Stop() {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.stopLocked()
}

func (m *Manager) UploadTotal() int64 {
	return statistic.DefaultManager.Snapshot().UploadTotal
}

func (m *Manager) DownloadTotal() int64 {
	return statistic.DefaultManager.Snapshot().DownloadTotal
}

func (m *Manager) stopLocked() {
	m.stopTunLocked()
	if m.xray != nil {
		_ = m.xray.Close()
		m.xray = nil
	}
	if m.injector != nil {
		m.injector.stop()
		m.injector = nil
	}
	m.tunProxy = nil
}

func (m *Manager) stopTunLocked() {
	if m.tun != nil {
		m.tun.stop()
		m.tun = nil
		m.log("tun2socks stopped")
	}
}

func (m *Manager) log(message string) {
	if m.platform != nil {
		m.platform.Log(message)
	}
}
