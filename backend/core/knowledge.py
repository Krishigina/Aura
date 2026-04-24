import io
import os
import zipfile
import xml.etree.ElementTree as ET

from fastapi import HTTPException
from pypdf import PdfReader


def get_knowledge_source_type(filename: str) -> str:
    ext = os.path.splitext(filename or "")[1].lower().lstrip(".")
    if ext in {"txt", "md", "pdf", "docx"}:
        return ext
    if ext == "doc":
        raise HTTPException(status_code=400, detail="Формат .doc не поддерживается. Сохраните документ как .docx и загрузите снова")
    raise HTTPException(status_code=400, detail="Поддерживаются только файлы PDF, TXT, MD и DOCX")


def extract_docx_text(content: bytes) -> str:
    try:
        with zipfile.ZipFile(io.BytesIO(content)) as archive:
            xml_content = archive.read("word/document.xml")
    except Exception as error:
        raise HTTPException(status_code=400, detail="Не удалось прочитать DOCX-документ") from error

    try:
        root = ET.fromstring(xml_content)
    except ET.ParseError as error:
        raise HTTPException(status_code=400, detail="Не удалось разобрать DOCX-документ") from error

    parts = []
    for node in root.iter():
        if node.tag.endswith("}t") and node.text:
            parts.append(node.text)
    return " ".join(parts).strip()


def extract_pdf_text(content: bytes) -> str:
    try:
        reader = PdfReader(io.BytesIO(content))
        return "\n".join((page.extract_text() or "") for page in reader.pages).strip()
    except Exception as error:
        raise HTTPException(status_code=400, detail="Не удалось прочитать PDF-документ") from error


def extract_knowledge_text(filename: str, content: bytes) -> str:
    source_type = get_knowledge_source_type(filename)
    if source_type in {"txt", "md"}:
        return content.decode("utf-8", errors="ignore").strip()
    if source_type == "docx":
        return extract_docx_text(content)
    if source_type == "pdf":
        return extract_pdf_text(content)
    raise HTTPException(status_code=400, detail="Неподдерживаемый формат файла")


def normalize_knowledge_scope(scope: str) -> str:
    if scope not in {"both", "rag", "recommendations"}:
        raise HTTPException(status_code=400, detail="Некорректная область использования источника")
    return scope


def knowledge_source_response(row):
    data = dict(row)
    data.pop("content", None)
    return data
