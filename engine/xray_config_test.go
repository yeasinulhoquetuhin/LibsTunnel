package vpncore

import (
	"encoding/json"
	"testing"
)

func TestPrepareXrayConfig(t *testing.T) {
	raw := json.RawMessage(`{"outbounds":[{"tag":"proxy","protocol":"vless","settings":{"vnext":[{"address":"server.example","port":443,"users":[{"id":"test-user"}]}]}}]}`)
	injection := &injectionConfig{Enabled: true}
	result, err := prepareXrayConfig(raw, "proxy", injection, "127.0.0.1:32123")
	if err != nil {
		t.Fatal(err)
	}
	if injection.TargetHost != "server.example" || injection.TargetPort != 443 {
		t.Fatalf("wrong derived target: %s:%d", injection.TargetHost, injection.TargetPort)
	}
	var root map[string]any
	if err := json.Unmarshal(result, &root); err != nil {
		t.Fatal(err)
	}
	outbound, _, err := findOutbound(root, "proxy")
	if err != nil {
		t.Fatal(err)
	}
	server, err := outboundServer(outbound, "vless")
	if err != nil {
		t.Fatal(err)
	}
	if server["address"] != "127.0.0.1" || jsonInt(server["port"]) != 32123 {
		t.Fatalf("outbound was not rewritten: %#v", server)
	}
}
