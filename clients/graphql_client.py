#!/usr/bin/env python3
"""
GraphQL client for the JPA4 API Server.

Supports: items, item, searchItems queries and createItem, updateItem,
deleteItem mutations — all against the /graphql endpoint.
Obtains a JWT token automatically before each run.

Usage examples:
  python graphql_client.py items
  python graphql_client.py item --id 1
  python graphql_client.py search --name widget
  python graphql_client.py create --name "Sprocket" --price 4.99 --quantity 50
  python graphql_client.py update --id 1 --price 3.49
  python graphql_client.py delete --id 5
"""

import argparse
import json
import sys
import urllib.request
import urllib.error


# ── HTTP helpers ──────────────────────────────────────────────────────────────

def _graphql(url: str, query: str, variables: dict, token: str) -> dict:
    payload = json.dumps({"query": query, "variables": variables}).encode()
    req = urllib.request.Request(url, data=payload, method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as resp:
            result = json.loads(resp.read())
    except urllib.error.HTTPError as exc:
        raw = exc.read()
        try:
            result = json.loads(raw)
        except Exception:
            print(f"HTTP {exc.code}: {raw.decode()}", file=sys.stderr)
            sys.exit(1)

    if "errors" in result:
        for err in result["errors"]:
            print(f"GraphQL error: {err.get('message')}", file=sys.stderr)
        sys.exit(1)

    return result.get("data", {})


def _request(method: str, url: str, body=None, headers=None) -> dict:
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as exc:
        raw = exc.read()
        try:
            err = json.loads(raw)
        except Exception:
            err = raw.decode()
        print(f"HTTP {exc.code}: {err}", file=sys.stderr)
        sys.exit(1)


# ── Auth ──────────────────────────────────────────────────────────────────────

def login(base_url: str, username: str, password: str) -> str:
    resp = _request("POST", f"{base_url}/auth/login",
                    body={"username": username, "password": password})
    token = resp.get("token")
    if not token:
        print("Login failed: no token in response", file=sys.stderr)
        sys.exit(1)
    return token


# ── GraphQL fragments ─────────────────────────────────────────────────────────

ITEM_FIELDS = """
  id
  name
  description
  price
  quantity
  createdAt
  updatedAt
"""


# ── Commands ──────────────────────────────────────────────────────────────────

def cmd_items(gql_url: str, token: str, _args) -> None:
    data = _graphql(gql_url, f"{{ items {{ {ITEM_FIELDS} }} }}", {}, token)
    _pretty(data["items"])


def cmd_item(gql_url: str, token: str, args) -> None:
    query = f"query($id: ID!) {{ item(id: $id) {{ {ITEM_FIELDS} }} }}"
    data = _graphql(gql_url, query, {"id": args.id}, token)
    _pretty(data["item"])


def cmd_search(gql_url: str, token: str, args) -> None:
    query = f"query($name: String!) {{ searchItems(name: $name) {{ {ITEM_FIELDS} }} }}"
    data = _graphql(gql_url, query, {"name": args.name}, token)
    _pretty(data["searchItems"])


def cmd_create(gql_url: str, token: str, args) -> None:
    query = f"""
    mutation($input: CreateItemInput!) {{
      createItem(input: $input) {{ {ITEM_FIELDS} }}
    }}
    """
    variables = {
        "input": {
            "name": args.name,
            "price": args.price,
            "quantity": args.quantity,
            **({"description": args.description} if args.description else {}),
        }
    }
    data = _graphql(gql_url, query, variables, token)
    _pretty(data["createItem"])


def cmd_update(gql_url: str, token: str, args) -> None:
    inp = {}
    if args.name:
        inp["name"] = args.name
    if args.description:
        inp["description"] = args.description
    if args.price is not None:
        inp["price"] = args.price
    if args.quantity is not None:
        inp["quantity"] = args.quantity
    if not inp:
        print("No fields to update provided.", file=sys.stderr)
        sys.exit(1)
    query = f"""
    mutation($id: ID!, $input: UpdateItemInput!) {{
      updateItem(id: $id, input: $input) {{ {ITEM_FIELDS} }}
    }}
    """
    data = _graphql(gql_url, query, {"id": args.id, "input": inp}, token)
    _pretty(data["updateItem"])


def cmd_delete(gql_url: str, token: str, args) -> None:
    query = "mutation($id: ID!) { deleteItem(id: $id) }"
    data = _graphql(gql_url, query, {"id": args.id}, token)
    deleted = data.get("deleteItem", False)
    print(f"Deleted: {deleted}")


def _pretty(obj) -> None:
    print(json.dumps(obj, indent=2))


# ── CLI ───────────────────────────────────────────────────────────────────────

def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="GraphQL client for the JPA4 API Server",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    p.add_argument("--url", default="http://localhost:8080",
                   help="Base server URL (default: http://localhost:8080)")
    p.add_argument("--username", default="admin", help="Login username")
    p.add_argument("--password", default="admin", help="Login password")

    sub = p.add_subparsers(dest="command", required=True, metavar="COMMAND")

    sub.add_parser("items", help="Query all items")

    p_item = sub.add_parser("item", help="Query single item by ID")
    p_item.add_argument("--id", required=True, help="Item ID")

    p_search = sub.add_parser("search", help="Search items by name fragment")
    p_search.add_argument("--name", required=True, help="Name fragment")

    p_create = sub.add_parser("create", help="Create a new item")
    p_create.add_argument("--name", required=True)
    p_create.add_argument("--price", type=float, required=True)
    p_create.add_argument("--quantity", type=int, required=True)
    p_create.add_argument("--description", default=None)

    p_update = sub.add_parser("update", help="Partially update an item")
    p_update.add_argument("--id", required=True)
    p_update.add_argument("--name", default=None)
    p_update.add_argument("--description", default=None)
    p_update.add_argument("--price", type=float, default=None)
    p_update.add_argument("--quantity", type=int, default=None)

    p_delete = sub.add_parser("delete", help="Delete an item by ID")
    p_delete.add_argument("--id", required=True)

    return p


COMMANDS = {
    "items": cmd_items,
    "item": cmd_item,
    "search": cmd_search,
    "create": cmd_create,
    "update": cmd_update,
    "delete": cmd_delete,
}


def main():
    parser = build_parser()
    args = parser.parse_args()

    token = login(args.url, args.username, args.password)
    gql_url = f"{args.url}/graphql"
    COMMANDS[args.command](gql_url, token, args)


if __name__ == "__main__":
    main()
