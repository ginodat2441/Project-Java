import os
import sys
from http.client import HTTPConnection, HTTPException
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit


PROXY_HOST = os.environ.get("PROXY_HOST", "127.0.0.1")
PROXY_PORT = int(os.environ.get("PROXY_PORT", "3000"))
FRONTEND_HOST = os.environ.get("FRONTEND_HOST", "127.0.0.1")
FRONTEND_PORT = int(os.environ.get("FRONTEND_PORT", "5500"))
BACKEND_HOST = os.environ.get("BACKEND_HOST", "127.0.0.1")
BACKEND_PORT = int(os.environ.get("BACKEND_PORT", "8080"))
HOP_BY_HOP_HEADERS = {
    "connection",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailer",
    "transfer-encoding",
    "upgrade",
}


class ProxyHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        self._dispatch()

    def do_HEAD(self):
        self._dispatch()

    def do_POST(self):
        self._dispatch()

    def do_PUT(self):
        self._dispatch()

    def do_PATCH(self):
        self._dispatch()

    def do_DELETE(self):
        self._dispatch()

    def do_OPTIONS(self):
        self._dispatch()

    def _dispatch(self):
        if self._is_api_request():
            self._proxy_request(BACKEND_HOST, BACKEND_PORT)
        else:
            self._proxy_request(FRONTEND_HOST, FRONTEND_PORT)

    def _is_api_request(self):
        path = urlsplit(self.path).path
        return path == "/api" or path.startswith("/api/")

    def _proxy_request(self, host, port):
        target = urlsplit(self.path)
        path = target.path or "/"
        if target.query:
            path += "?" + target.query

        connection_headers = {
            token.strip().lower()
            for value in self.headers.get_all("Connection", [])
            for token in value.split(",")
        }
        excluded_headers = HOP_BY_HOP_HEADERS | connection_headers
        headers = {
            name: value
            for name, value in self.headers.items()
            if name.lower() not in excluded_headers and name.lower() != "host"
        }
        headers["Host"] = f"{host}:{port}"

        transfer_encoding = self.headers.get("Transfer-Encoding", "").lower()
        if transfer_encoding and transfer_encoding != "identity":
            self.send_error(501, "Chunked request bodies are not supported")
            return

        content_length = self.headers.get("Content-Length")
        try:
            body_length = int(content_length) if content_length is not None else None
        except ValueError:
            self.send_error(400, "Invalid Content-Length")
            return
        if body_length is not None and body_length < 0:
            self.send_error(400, "Invalid Content-Length")
            return
        body = self.rfile.read(body_length) if body_length is not None else None

        connection = HTTPConnection(host, port, timeout=30)
        try:
            try:
                connection.request(self.command, path, body=body, headers=headers)
                response = connection.getresponse()
            except (HTTPException, OSError) as error:
                self.send_error(502, f"Backend unavailable: {error}")
                return

            response_connection_headers = {
                token.strip().lower()
                for value in response.headers.get_all("Connection", [])
                for token in value.split(",")
            }
            excluded_response_headers = HOP_BY_HOP_HEADERS | response_connection_headers
            suppress_response_body = self.command == "HEAD" or response.status in (204, 205, 304)
            send_chunked_response = (
                not suppress_response_body and response.getheader("Content-Length") is None
            )

            self.send_response(response.status, response.reason)
            for name, value in response.getheaders():
                if name.lower() not in excluded_response_headers:
                    if send_chunked_response and name.lower() == "content-length":
                        continue
                    self.send_header(name, value)
            if send_chunked_response:
                self.send_header("Transfer-Encoding", "chunked")
            self.end_headers()

            if not suppress_response_body:
                while True:
                    chunk = response.read(64 * 1024)
                    if not chunk:
                        break
                    if send_chunked_response:
                        self.wfile.write(f"{len(chunk):X}\r\n".encode("ascii"))
                        self.wfile.write(chunk)
                        self.wfile.write(b"\r\n")
                    else:
                        self.wfile.write(chunk)
                if send_chunked_response:
                    self.wfile.write(b"0\r\n\r\n")
        finally:
            connection.close()

    def log_request(self, code="-", size="-"):
        path = urlsplit(self.path).path
        target = "backend" if self._is_api_request() else "frontend"
        print(f'{self.client_address[0]} "{self.command} {path}" {code} -> {target}', flush=True)


def main():
    try:
        server = ThreadingHTTPServer((PROXY_HOST, PROXY_PORT), ProxyHandler)
    except OSError as error:
        print(f"Cannot start proxy on {PROXY_HOST}:{PROXY_PORT}: {error}", file=sys.stderr)
        return 1

    server.daemon_threads = True
    print(f"Proxy listening at http://{PROXY_HOST}:{PROXY_PORT}")
    print(f"Frontend requests are forwarded to http://{FRONTEND_HOST}:{FRONTEND_PORT}")
    print(f"API requests are forwarded to http://{BACKEND_HOST}:{BACKEND_PORT}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nProxy stopped.")
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
