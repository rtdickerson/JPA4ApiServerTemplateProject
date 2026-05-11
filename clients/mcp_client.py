#!/usr/bin/env python3
"""
MCP client for the JPA4 API Server.

Uses the SSE transport: connects to GET /mcp/sse to receive server events,
then posts JSON-RPC tool-call requests to the session endpoint returned
by the server.  A JWT token is fetched automatically (for parity with the
REST and GraphQL clients) but the MCP endpoints are currently open.

Supports: list, get, search, create, update, delete operations via MCP tools.

Usage examples:
  python mcp_client.py list
  python mcp_client.py get --id 1
  python mcp_client.py search --name widget
  python mcp_client.py create --name "Sprocket" --price 4.99 --quantity 50
  python mcp_client.py update --id 1 --price 3.49
  python mcp_client.py delete --id 5
  python mcp_client.py tools          # list all available MCP tools
"""

import argparse
import json
import queue
import sys
import threading
import urllib.error
import urllib.request


# ── SSE reader ────────────────────────────────────────────────────────────────

class SseReader:
    """Reads a Server-Sent Events stream in a background thread.

    Parsed events are placed on self.events as (event_type, data) tuples.
    """

    def __init__(self, url: str):
        self.url = url
        self.events: queue.Queue = queue.Queue()
        self._stop = threading.Event()
        self._thread = threading.Thread(target=self._run, daemon=True)

    def start(self):
        self._thread.start()

    def stop(self):
        self._stop.set()

    def _run(self):
        req = urllib.request.Request(self.url)
        req.add_header("Accept", "text/event-stream")
        req.add_header("Cache-Control", "no-cache")
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                self._parse(resp)
        except Exception as exc:
            self.events.put(("_error", str(exc)))

    def _parse(self, stream):
        event_type = "message"
        buf = []
        for raw in stream:
            if self._stop.is_set():
                break
            line = raw.decode("utf-8").rstrip("\n").rstrip("\r")
            if line.startswith("event:"):
                event_type = line[6:].strip()
            elif line.startswith("data:"):
                buf.append(line[5:].strip())
            elif line == "":
                if buf:
                    self.events.put((event_type, "\n".join(buf)))
                    buf = []
                    event_type = "message"


# ── JSON-RPC helpers ──────────────────────────────────────────────────────────

_rpc_id = 0


def _next_id() -> int:
    global _rpc_id
    _rpc_id += 1
    return _rpc_id


def _rpc(method: str, params: dict | None = None) -> dict:
    msg = {"jsonrpc": "2.0", "id": _next_id(), "method": method}
    if params is not None:
        msg["params"] = params
    return msg


def _notify(method: str, params: dict | None = None) -> dict:
    msg = {"jsonrpc": "2.0", "method": method}
    if params is not None:
        msg["params"] = params
    return msg


def _post(url: str, body: dict) -> None:
    data = json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req) as resp:
            resp.read()
    except urllib.error.HTTPError as exc:
        raw = exc.read()
        print(f"POST {url} → HTTP {exc.code}: {raw.decode()}", file=sys.stderr)
        sys.exit(1)


def _wait_response(events: queue.Queue, rpc_id: int, timeout: float = 10.0) -> dict:
    """Block until the SSE stream delivers the JSON-RPC response for rpc_id."""
    import time
    deadline = time.monotonic() + timeout
    while True:
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            print("Timed out waiting for MCP response.", file=sys.stderr)
            sys.exit(1)
        try:
            ev_type, ev_data = events.get(timeout=min(remaining, 1.0))
        except queue.Empty:
            continue
        if ev_type == "_error":
            print(f"SSE error: {ev_data}", file=sys.stderr)
            sys.exit(1)
        try:
            msg = json.loads(ev_data)
        except json.JSONDecodeError:
            continue
        if msg.get("id") == rpc_id:
            if "error" in msg:
                print(f"RPC error: {msg['error']}", file=sys.stderr)
                sys.exit(1)
            return msg.get("result", {})


# ── MCP session ───────────────────────────────────────────────────────────────

class McpSession:
    """Manages a single MCP SSE session lifecycle."""

    def __init__(self, base_url: str):
        self._sse = SseReader(f"{base_url}/mcp/sse")
        self._sse.start()
        self._msg_url = self._handshake(base_url)

    def _handshake(self, base_url: str) -> str:
        # Wait for the "endpoint" event that carries the session message URL.
        ev_type, ev_data = self._sse.events.get(timeout=10)
        if ev_type == "_error":
            print(f"SSE connection error: {ev_data}", file=sys.stderr)
            sys.exit(1)
        if ev_type != "endpoint":
            print(f"Unexpected first SSE event type '{ev_type}': {ev_data}",
                  file=sys.stderr)
            sys.exit(1)

        # ev_data is a relative path like /mcp/message?sessionId=xxx
        msg_url = base_url.rstrip("/") + ev_data.strip()

        # Initialize the MCP session.
        init_id = _next_id()
        _post(msg_url, {
            "jsonrpc": "2.0",
            "id": init_id,
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "python-mcp-client", "version": "1.0.0"},
            },
        })
        _wait_response(self._sse.events, init_id)

        # Acknowledge initialization.
        _post(msg_url, _notify("notifications/initialized"))

        return msg_url

    def call_tool(self, name: str, arguments: dict) -> str:
        msg = _rpc("tools/call", {"name": name, "arguments": arguments})
        _post(self._msg_url, msg)
        result = _wait_response(self._sse.events, msg["id"])
        contents = result.get("content", [])
        texts = [c.get("text", "") for c in contents if c.get("type") == "text"]
        return "\n".join(texts)

    def list_tools(self) -> list:
        msg = _rpc("tools/list")
        _post(self._msg_url, msg)
        result = _wait_response(self._sse.events, msg["id"])
        return result.get("tools", [])

    def close(self):
        self._sse.stop()


# ── Auth ──────────────────────────────────────────────────────────────────────

def login(base_url: str, username: str, password: str) -> str:
    body = json.dumps({"username": username, "password": password}).encode()
    req = urllib.request.Request(f"{base_url}/auth/login", data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read())
            token = data.get("token")
            if not token:
                print("Login failed: no token in response", file=sys.stderr)
                sys.exit(1)
            return token
    except urllib.error.HTTPError as exc:
        print(f"Login HTTP {exc.code}: {exc.read().decode()}", file=sys.stderr)
        sys.exit(1)


# ── Commands ──────────────────────────────────────────────────────────────────

def cmd_tools(session: McpSession, _args) -> None:
    tools = session.list_tools()
    for t in tools:
        print(f"  {t['name']:20s}  {t.get('description', '')}")


def cmd_list(session: McpSession, _args) -> None:
    raw = session.call_tool("list_items", {})
    _pretty_json(raw)


def cmd_get(session: McpSession, args) -> None:
    raw = session.call_tool("get_item", {"id": args.id})
    _pretty_json(raw)


def cmd_search(session: McpSession, args) -> None:
    raw = session.call_tool("search_items", {"name": args.name})
    _pretty_json(raw)


def cmd_create(session: McpSession, args) -> None:
    params = {"name": args.name, "price": args.price, "quantity": args.quantity}
    if args.description:
        params["description"] = args.description
    raw = session.call_tool("create_item", params)
    _pretty_json(raw)


def cmd_update(session: McpSession, args) -> None:
    params: dict = {"id": args.id}
    if args.name:
        params["name"] = args.name
    if args.description:
        params["description"] = args.description
    if args.price is not None:
        params["price"] = args.price
    if args.quantity is not None:
        params["quantity"] = args.quantity
    if len(params) == 1:
        print("No fields to update provided.", file=sys.stderr)
        sys.exit(1)
    raw = session.call_tool("update_item", params)
    _pretty_json(raw)


def cmd_delete(session: McpSession, args) -> None:
    raw = session.call_tool("delete_item", {"id": args.id})
    print(f"Deleted: {raw.strip()}")


def _pretty_json(text: str) -> None:
    try:
        print(json.dumps(json.loads(text), indent=2))
    except json.JSONDecodeError:
        print(text)


# ── CLI ───────────────────────────────────────────────────────────────────────

def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="MCP client for the JPA4 API Server (SSE transport)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    p.add_argument("--url", default="http://localhost:8080",
                   help="Base server URL (default: http://localhost:8080)")
    p.add_argument("--username", default="admin", help="Login username")
    p.add_argument("--password", default="admin", help="Login password")

    sub = p.add_subparsers(dest="command", required=True, metavar="COMMAND")

    sub.add_parser("tools", help="List all available MCP tools")
    sub.add_parser("list", help="List all items via MCP")

    p_get = sub.add_parser("get", help="Get item by ID via MCP")
    p_get.add_argument("--id", type=int, required=True)

    p_search = sub.add_parser("search", help="Search items by name fragment via MCP")
    p_search.add_argument("--name", required=True)

    p_create = sub.add_parser("create", help="Create a new item via MCP")
    p_create.add_argument("--name", required=True)
    p_create.add_argument("--price", type=float, required=True)
    p_create.add_argument("--quantity", type=int, required=True)
    p_create.add_argument("--description", default=None)

    p_update = sub.add_parser("update", help="Partially update an item via MCP")
    p_update.add_argument("--id", type=int, required=True)
    p_update.add_argument("--name", default=None)
    p_update.add_argument("--description", default=None)
    p_update.add_argument("--price", type=float, default=None)
    p_update.add_argument("--quantity", type=int, default=None)

    p_delete = sub.add_parser("delete", help="Delete an item by ID via MCP")
    p_delete.add_argument("--id", type=int, required=True)

    return p


COMMANDS = {
    "tools": cmd_tools,
    "list": cmd_list,
    "get": cmd_get,
    "search": cmd_search,
    "create": cmd_create,
    "update": cmd_update,
    "delete": cmd_delete,
}


def main():
    parser = build_parser()
    args = parser.parse_args()

    # Obtain token for parity with other clients (MCP endpoints are currently open).
    login(args.url, args.username, args.password)

    session = McpSession(args.url)
    try:
        COMMANDS[args.command](session, args)
    finally:
        session.close()


if __name__ == "__main__":
    main()
