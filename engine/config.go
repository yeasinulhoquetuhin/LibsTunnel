package vpncore

import (
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"time"
)

type config struct {
	Backend     string          `json:"backend,omitempty"`
	Xray        json.RawMessage `json:"xray"`
	OutboundTag string          `json:"outboundTag,omitempty"`
	Injection   injectionConfig `json:"injection"`
	Tun         tunConfig       `json:"tun,omitempty"`
	SSH         sshConfig       `json:"ssh,omitempty"`
}

type sshConfig struct {
	Host          string `json:"host"`
	Port          int    `json:"port"`
	Username      string `json:"username"`
	Password      string `json:"password,omitempty"`
	PrivateKey    string `json:"privateKeyFile,omitempty"`
	KeyPassphrase string `json:"privateKeyPassphrase,omitempty"`
}

type tunConfig struct {
	SocksAddress string `json:"socksAddress,omitempty"`
	MTU          int    `json:"mtu,omitempty"`
}

type injectionConfig struct {
	Enabled               bool   `json:"enabled"`
	Mode                  string `json:"mode"`
	TargetHost            string `json:"targetHost,omitempty"`
	TargetPort            int    `json:"targetPort,omitempty"`
	ProxyHost             string `json:"proxyHost,omitempty"`
	ProxyPort             int    `json:"proxyPort,omitempty"`
	ServerNameIndication  string `json:"serverNameIndication,omitempty"`
	AllowInsecure         bool   `json:"allowInsecure,omitempty"`
	Payload               string `json:"payload,omitempty"`
	Method                string `json:"method,omitempty"`
	Protocol              string `json:"protocol,omitempty"`
	UserAgent             string `json:"userAgent,omitempty"`
	ResponseMode          string `json:"responseMode,omitempty"`
	AcceptedStatus        []int  `json:"acceptedStatus,omitempty"`
	AcceptAnyResponse     bool   `json:"acceptAnyResponse,omitempty"`
	ConnectTimeoutMillis  int    `json:"connectTimeoutMillis,omitempty"`
	ResponseTimeoutMillis int    `json:"responseTimeoutMillis,omitempty"`
}

const (
	backendXray = "xray"
	backendSSH  = "ssh"

	modeDirectTarget = "direct"
	modeDirectSNI    = "direct-sni"
	modeProxyTarget  = "proxy"
	modeProxySNI     = "proxy-sni"

	responseNone    = "none"
	responseHeaders = "headers"
)

func parseConfig(raw string) (*config, error) {
	var cfg config
	if err := json.Unmarshal([]byte(raw), &cfg); err != nil {
		return nil, fmt.Errorf("decode engine config: %w", err)
	}
	if err := cfg.normalizeAndValidate(); err != nil {
		return nil, err
	}
	return &cfg, nil
}

func (c *config) normalizeAndValidate() error {
	c.Backend = strings.ToLower(strings.TrimSpace(c.Backend))
	if c.Backend == "" {
		c.Backend = backendXray
	}
	switch c.Backend {
	case backendXray:
		if len(c.Xray) == 0 || string(c.Xray) == "null" {
			return errors.New("xray config is required")
		}
		var probe map[string]any
		if err := json.Unmarshal(c.Xray, &probe); err != nil {
			return fmt.Errorf("xray must be a JSON object: %w", err)
		}
	case backendSSH:
		if c.SSH.Host == "" || c.SSH.Port < 1 || c.SSH.Port > 65535 {
			return errors.New("ssh backend requires a valid host and port")
		}
		if c.SSH.Username == "" {
			return errors.New("ssh backend requires a username")
		}
		if c.SSH.Password == "" && c.SSH.PrivateKey == "" {
			return errors.New("ssh backend requires a password or private key")
		}
		if c.Injection.Enabled {
			return errors.New("payload injection is only supported by the xray backend")
		}
	default:
		return fmt.Errorf("unsupported backend %q", c.Backend)
	}
	if c.Tun.SocksAddress == "" {
		c.Tun.SocksAddress = "127.0.0.1:10808"
	}
	if c.Tun.MTU == 0 {
		c.Tun.MTU = 1500
	}
	if c.Tun.MTU < 1280 || c.Tun.MTU > 9000 {
		return errors.New("tun MTU must be between 1280 and 9000")
	}

	i := &c.Injection
	if !i.Enabled {
		return nil
	}
	i.Mode = strings.ToLower(strings.TrimSpace(i.Mode))
	if i.Mode == "" {
		i.Mode = modeDirectTarget
	}
	switch i.Mode {
	case modeDirectTarget, modeDirectSNI, modeProxyTarget, modeProxySNI:
	default:
		return fmt.Errorf("unsupported injection mode %q", i.Mode)
	}
	if i.Method == "" {
		i.Method = "CONNECT"
	}
	if i.Protocol == "" {
		i.Protocol = "HTTP/1.1"
	}
	if i.UserAgent == "" {
		i.UserAgent = "libs-tunnel/1.0"
	}
	if i.ResponseMode == "" {
		if i.Payload == "" {
			i.ResponseMode = responseNone
		} else {
			i.ResponseMode = responseHeaders
		}
	}
	switch i.ResponseMode {
	case responseNone, responseHeaders:
	default:
		return fmt.Errorf("unsupported responseMode %q", i.ResponseMode)
	}
	if len(i.AcceptedStatus) == 0 {
		i.AcceptedStatus = []int{101, 200}
	}
	if i.ConnectTimeoutMillis <= 0 {
		i.ConnectTimeoutMillis = int((12 * time.Second) / time.Millisecond)
	}
	if i.ResponseTimeoutMillis <= 0 {
		i.ResponseTimeoutMillis = int((8 * time.Second) / time.Millisecond)
	}
	if strings.HasPrefix(i.Mode, "proxy") {
		if i.ProxyHost == "" || i.ProxyPort < 1 || i.ProxyPort > 65535 {
			return errors.New("proxy mode requires a valid proxyHost and proxyPort")
		}
	}
	return nil
}
