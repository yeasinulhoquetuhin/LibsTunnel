package vpncore

import (
	"testing"
	"time"
)

func TestExpandPayload(t *testing.T) {
	chunks, err := expandPayload(
		"[method] [host_port] [protocol][crlf]Host: [host][crlf][split=25]X-UA: [ua][crlf][crlf]",
		payloadVariables{Host: "example.com", Port: 443, Method: "CONNECT", Protocol: "HTTP/1.1", UserAgent: "test"},
	)
	if err != nil {
		t.Fatal(err)
	}
	if len(chunks) != 2 {
		t.Fatalf("got %d chunks", len(chunks))
	}
	if got := string(chunks[0].data); got != "CONNECT example.com:443 HTTP/1.1\r\nHost: example.com\r\n" {
		t.Fatalf("unexpected first chunk: %q", got)
	}
	if chunks[1].delayBefore != 25*time.Millisecond {
		t.Fatalf("unexpected delay: %v", chunks[1].delayBefore)
	}
}
