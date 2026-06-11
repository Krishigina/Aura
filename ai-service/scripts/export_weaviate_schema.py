import argparse
import json
import re
import sys
from pathlib import Path
from urllib.error import URLError, HTTPError
from urllib.request import urlopen


ROOT = Path(__file__).resolve().parents[2]
VECTOR_STORE_PATH = ROOT / "ai-service" / "app" / "infrastructure" / "vector_store.py"
CONFIG_PATH = ROOT / "ai-service" / "app" / "core" / "config.py"
DEFAULT_JSON_OUT = ROOT / "docs" / "weaviate_schema.json"
DEFAULT_MD_OUT = ROOT / "docs" / "weaviate_schema.md"


def fetch_live_schema(base_url: str) -> dict:
    schema_url = base_url.rstrip("/") + "/v1/schema"
    with urlopen(schema_url, timeout=20) as response:
        return json.loads(response.read().decode("utf-8"))


def parse_code_schema() -> dict:
    vector_store_source = VECTOR_STORE_PATH.read_text(encoding="utf-8")
    config_source = CONFIG_PATH.read_text(encoding="utf-8")

    collection_names = re.findall(r'collection_\w+:\s*str\s*=\s*"([^"]+)"', config_source)
    property_matches = re.findall(
        r'Property\(name="([^"]+)",\s*data_type=DataType\.([A-Z]+)\)',
        vector_store_source,
    )

    properties = [{"name": name, "dataType": [data_type]} for name, data_type in property_matches]

    classes = []
    for name in collection_names:
        classes.append(
            {
                "class": name,
                "vectorizer": "none",
                "properties": properties,
            }
        )

    return {"classes": classes}


def normalize_schema(schema: dict, source_kind: str, source_url: str | None) -> dict:
    classes = schema.get("classes", [])
    collections = []
    for item in classes:
        raw_properties = item.get("properties") or []
        properties = []
        for prop in raw_properties:
            data_type = prop.get("dataType") or []
            dtype = data_type[0] if data_type else "UNKNOWN"
            properties.append(
                {
                    "name": prop.get("name"),
                    "type": dtype,
                }
            )
        collections.append(
            {
                "name": item.get("class") or item.get("name"),
                "vectorizer": item.get("vectorizer") or "unknown",
                "properties": properties,
            }
        )

    return {
        "source_date": __import__("datetime").date.today().isoformat(),
        "source_kind": source_kind,
        "source_url": source_url,
        "source_files": [
            str(VECTOR_STORE_PATH),
            str(CONFIG_PATH),
        ],
        "collections": collections,
    }


def render_markdown(export_data: dict) -> str:
    lines = []
    lines.append("# Weaviate Schema Export")
    lines.append("")
    lines.append(f"Source date: {export_data['source_date']}")
    lines.append(f"Source kind: `{export_data['source_kind']}`")
    if export_data.get("source_url"):
        lines.append(f"Source URL: `{export_data['source_url']}`")
    lines.append("")
    lines.append("## Collections")
    lines.append("")
    for collection in export_data["collections"]:
        lines.append(f"### `{collection['name']}`")
        lines.append(f"- vectorizer: `{collection['vectorizer']}`")
        lines.append("")
        lines.append("| Property | Type |")
        lines.append("|---|---|")
        for prop in collection["properties"]:
            lines.append(f"| `{prop['name']}` | `{prop['type']}` |")
        lines.append("")
    return "\n".join(lines) + "\n"


def write_outputs(export_data: dict, json_out: Path, md_out: Path) -> None:
    json_out.parent.mkdir(parents=True, exist_ok=True)
    md_out.parent.mkdir(parents=True, exist_ok=True)
    json_out.write_text(json.dumps(export_data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    md_out.write_text(render_markdown(export_data), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Export Weaviate schema from live server or code fallback.")
    parser.add_argument("--url", default="http://localhost:8080", help="Weaviate base URL")
    parser.add_argument("--json-out", default=str(DEFAULT_JSON_OUT), help="Output JSON path")
    parser.add_argument("--md-out", default=str(DEFAULT_MD_OUT), help="Output Markdown path")
    parser.add_argument(
        "--fallback-code",
        action="store_true",
        help="If live Weaviate is unavailable, export schema from code definitions instead.",
    )
    parser.add_argument(
        "--code-only",
        action="store_true",
        help="Skip live Weaviate and export schema from code definitions only.",
    )
    args = parser.parse_args()

    json_out = Path(args.json_out)
    md_out = Path(args.md_out)

    try:
        if args.code_only:
            raw_schema = parse_code_schema()
            export_data = normalize_schema(raw_schema, "code-derived", None)
        else:
            raw_schema = fetch_live_schema(args.url)
            export_data = normalize_schema(raw_schema, "live-weaviate", args.url.rstrip("/") + "/v1/schema")
    except (URLError, HTTPError, TimeoutError, OSError) as error:
        if not args.fallback_code:
            print(f"Live Weaviate export failed: {error}", file=sys.stderr)
            return 1
        raw_schema = parse_code_schema()
        export_data = normalize_schema(raw_schema, "code-derived", None)

    write_outputs(export_data, json_out, md_out)
    print(json_out)
    print(md_out)
    print(f"collections={len(export_data['collections'])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
