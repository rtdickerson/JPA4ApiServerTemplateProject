#!/usr/bin/env python3
"""
REST client for the JPA4 API Server.

Supports: list, get, search, create, update, delete operations on /api/items.
Obtains a JWT token automatically before each run.

Usage examples:
  python rest_client.py list
  python rest_client.py get --id 1
  python rest_client.py search --query widget
  python rest_client.py create --name "Sprocket" --price 4.99 --quantity 50
  python rest_client.py update --id 1 --price 3.49
  python rest_client.py delete --id 5
"""

import argparse
import json
import sys
import urllib.request
import urllib.error


# ── HTTP helpers ─────────────────────────────────────────────────────────────

def _request(method: str, url: str, body=None, headers=None) -> dict | list | str:
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as exc:
        raw = exc.read()
        try:
            err = json.loads(raw)
        except Exception:
            err = raw.decode()
        print(f"HTTP {exc.code}: {err}", file=sys.stderr)
        sys.exit(1)


def _auth_header(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


# ── Auth ─────────────────────────────────────────────────────────────────────

def login(base_url: str, username: str, password: str) -> str:
    resp = _request("POST", f"{base_url}/auth/login",
                    body={"username": username, "password": password})
    token = resp.get("token")
    if not token:
        print("Login failed: no token in response", file=sys.stderr)
        sys.exit(1)
    return token


# ── Commands ─────────────────────────────────────────────────────────────────

def cmd_list(base_url: str, token: str, _args) -> None:
    items = _request("GET", f"{base_url}/api/items",
                     headers=_auth_header(token))
    _pretty(items)


def cmd_get(base_url: str, token: str, args) -> None:
    item = _request("GET", f"{base_url}/api/items/{args.id}",
                    headers=_auth_header(token))
    _pretty(item)


def cmd_search(base_url: str, token: str, args) -> None:
    items = _request("GET", f"{base_url}/api/items?search={args.query}",
                     headers=_auth_header(token))
    _pretty(items)


def cmd_create(base_url: str, token: str, args) -> None:
    body = {"name": args.name, "price": args.price, "quantity": args.quantity}
    if args.description:
        body["description"] = args.description
    item = _request("POST", f"{base_url}/api/items", body=body,
                    headers=_auth_header(token))
    _pretty(item)


def cmd_update(base_url: str, token: str, args) -> None:
    body = {}
    if args.name:
        body["name"] = args.name
    if args.description:
        body["description"] = args.description
    if args.price is not None:
        body["price"] = args.price
    if args.quantity is not None:
        body["quantity"] = args.quantity
    if not body:
        print("No fields to update provided.", file=sys.stderr)
        sys.exit(1)
    item = _request("PUT", f"{base_url}/api/items/{args.id}", body=body,
                    headers=_auth_header(token))
    _pretty(item)


def cmd_delete(base_url: str, token: str, args) -> None:
    _request("DELETE", f"{base_url}/api/items/{args.id}",
             headers=_auth_header(token))
    print(f"Item {args.id} deleted.")


def _pretty(obj) -> None:
    print(json.dumps(obj, indent=2))


# ── CLI ───────────────────────────────────────────────────────────────────────

def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="REST client for the JPA4 API Server",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    p.add_argument("--url", default="http://localhost:8080",
                   help="Base server URL (default: http://localhost:8080)")
    p.add_argument("--username", default="admin", help="Login username")
    p.add_argument("--password", default="admin", help="Login password")

    sub = p.add_subparsers(dest="command", required=True, metavar="COMMAND")

    sub.add_parser("list", help="List all items")

    p_get = sub.add_parser("get", help="Get item by ID")
    p_get.add_argument("--id", type=int, required=True, help="Item ID")

    p_search = sub.add_parser("search", help="Search items by name fragment")
    p_search.add_argument("--query", required=True, help="Name fragment to search")

    p_create = sub.add_parser("create", help="Create a new item")
    p_create.add_argument("--name", required=True)
    p_create.add_argument("--price", type=float, required=True)
    p_create.add_argument("--quantity", type=int, required=True)
    p_create.add_argument("--description", default=None)

    p_update = sub.add_parser("update", help="Partially update an item")
    p_update.add_argument("--id", type=int, required=True)
    p_update.add_argument("--name", default=None)
    p_update.add_argument("--description", default=None)
    p_update.add_argument("--price", type=float, default=None)
    p_update.add_argument("--quantity", type=int, default=None)

    p_delete = sub.add_parser("delete", help="Delete an item by ID")
    p_delete.add_argument("--id", type=int, required=True)

    return p


COMMANDS = {
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

    token = login(args.url, args.username, args.password)
    COMMANDS[args.command](args.url, token, args)


if __name__ == "__main__":
    main()
