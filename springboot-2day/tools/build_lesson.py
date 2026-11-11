#!/usr/bin/env python3
"""
Builds a lesson README from a source file, so the code blocks in the lesson are COPIED from the reference project
(never retyped) and can never drift from code that compiles and passes its tests.

In the source Markdown, a line of the form
    {{FILE relative/path/File.java}}
is replaced by
    **File:** `relative/path/File.java`
    ```<language>
    <exact content of reference/lesson-NN/relative/path/File.java>
    ```
Everything else is copied unchanged.

Usage: python tools/build_lesson.py <source.md> <reference dir> <output README.md>
"""
import os, re, sys

LANG = {".java": "java", ".xml": "xml", ".yml": "yaml", ".yaml": "yaml", ".json": "json", ".properties": "properties",
        ".sql": "sql", ".md": "markdown", ".sh": "bash", ".ps1": "powershell"}


def lang_for(path):
    name = os.path.basename(path)
    if name == "Dockerfile" or name.startswith("Dockerfile."):
        return "dockerfile"
    if name == ".dockerignore" or name == ".gitignore":
        return "text"
    return LANG.get(os.path.splitext(path)[1], "text")


def main():
    src, ref, out = sys.argv[1:4]
    text = open(src, encoding="utf-8").read()

    def expand(m):
        rel = m.group(1).strip()
        full = os.path.join(ref, *rel.split("/"))
        if not os.path.isfile(full):
            sys.exit(f"missing reference file: {rel}")
        body = open(full, encoding="utf-8").read().replace("\r\n", "\n").rstrip("\n")
        return f"**File:** `{rel}`\n```{lang_for(rel)}\n{body}\n```"

    result = re.sub(r"^\{\{FILE ([^}]+)\}\}\s*$", expand, text, flags=re.M)
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    open(out, "w", encoding="utf-8", newline="\n").write(result)
    print("wrote", out, f"({result.count(chr(10))} lines)")


if __name__ == "__main__":
    main()
