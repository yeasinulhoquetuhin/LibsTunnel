package vpncore

import (
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"strconv"
)

func prepareXrayConfig(raw json.RawMessage, outboundTag string, injection *injectionConfig, injectorAddress string) ([]byte, error) {
	var root map[string]any
	if err := json.Unmarshal(raw, &root); err != nil {
		return nil, fmt.Errorf("decode xray config: %w", err)
	}
	if !injection.Enabled {
		return json.Marshal(root)
	}

	outbound, protocol, err := findOutbound(root, outboundTag)
	if err != nil {
		return nil, err
	}
	server, err := outboundServer(outbound, protocol)
	if err != nil {
		return nil, err
	}
	if injection.TargetHost == "" {
		injection.TargetHost, _ = server["address"].(string)
	}
	if injection.TargetPort == 0 {
		injection.TargetPort = jsonInt(server["port"])
	}
	if injection.TargetHost == "" || injection.TargetPort < 1 || injection.TargetPort > 65535 {
		return nil, errors.New("unable to derive a valid injection target from xray outbound")
	}

	localHost, localPortText, err := net.SplitHostPort(injectorAddress)
	if err != nil {
		return nil, fmt.Errorf("invalid injector address: %w", err)
	}
	localPort, err := strconv.Atoi(localPortText)
	if err != nil {
		return nil, fmt.Errorf("invalid injector port: %w", err)
	}
	server["address"] = localHost
	server["port"] = localPort
	return json.Marshal(root)
}

func findOutbound(root map[string]any, tag string) (map[string]any, string, error) {
	values, ok := root["outbounds"].([]any)
	if !ok || len(values) == 0 {
		return nil, "", errors.New("xray outbounds array is missing")
	}
	for _, value := range values {
		outbound, ok := value.(map[string]any)
		if !ok {
			continue
		}
		protocol, _ := outbound["protocol"].(string)
		outboundTag, _ := outbound["tag"].(string)
		if tag != "" && outboundTag != tag {
			continue
		}
		switch protocol {
		case "vless", "vmess", "trojan", "shadowsocks":
			return outbound, protocol, nil
		}
	}
	if tag != "" {
		return nil, "", fmt.Errorf("supported outbound with tag %q not found", tag)
	}
	return nil, "", errors.New("no VLESS, VMess, Trojan, or Shadowsocks outbound found")
}

func outboundServer(outbound map[string]any, protocol string) (map[string]any, error) {
	settings, ok := outbound["settings"].(map[string]any)
	if !ok {
		return nil, errors.New("outbound settings object is missing")
	}
	key := "servers"
	if protocol == "vless" || protocol == "vmess" {
		key = "vnext"
	}
	values, ok := settings[key].([]any)
	if !ok || len(values) == 0 {
		return nil, fmt.Errorf("outbound settings.%s is missing", key)
	}
	server, ok := values[0].(map[string]any)
	if !ok {
		return nil, fmt.Errorf("outbound settings.%s[0] is invalid", key)
	}
	return server, nil
}

func jsonInt(value any) int {
	switch number := value.(type) {
	case float64:
		return int(number)
	case int:
		return number
	case json.Number:
		result, _ := number.Int64()
		return int(result)
	default:
		return 0
	}
}
