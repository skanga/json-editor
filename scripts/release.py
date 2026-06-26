#!/usr/bin/env python3
from __future__ import annotations

import shutil
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def run(*args: str, capture: bool = False) -> str:
    result = subprocess.run(
        args,
        cwd=ROOT,
        check=True,
        text=True,
        stdout=subprocess.PIPE if capture else None,
    )
    return result.stdout.strip() if capture else ""


def require_clean_worktree() -> None:
    status = run("git", "status", "--porcelain", capture=True)
    if status:
        raise SystemExit("Working tree has uncommitted changes")


def maven_version() -> str:
    output = run(
        "mvn",
        "-q",
        "-DforceStdout",
        "help:evaluate",
        "-Dexpression=project.version",
        capture=True,
    )
    lines = [line.strip() for line in output.splitlines() if line.strip()]
    version = lines[-1] if lines else ""
    if not version:
        raise SystemExit("Could not read Maven project.version")
    return version


def main() -> None:
    require_clean_worktree()
    version = maven_version()
    tag = f"v{version}"
    local_jar = ROOT / "target" / "json-editor.jar"
    asset = ROOT / "target" / f"json-editor-{version}.jar"

    run("mvn", "-q", "clean", "package")
    if not local_jar.exists():
        raise SystemExit(f"Expected local JAR not found: {local_jar}")
    shutil.copyfile(local_jar, asset)

    require_clean_worktree()
    run("git", "tag", tag)
    run("git", "push", "origin", tag)
    run(
        "gh",
        "release",
        "create",
        tag,
        str(asset),
        "--title",
        tag,
        "--notes-file",
        "CHANGELOG.md",
    )


if __name__ == "__main__":
    main()
