#!/usr/bin/env python3
from __future__ import annotations

import os
import shutil
import subprocess
import sys
from pathlib import Path
from xml.etree import ElementTree


ROOT = Path(__file__).resolve().parents[1]


def existing(paths: list[Path]) -> list[str]:
    return [str(path) for path in paths if path.exists()]


def maven_candidates() -> list[str]:
    names = ["mvn.cmd", "mvn.bat"] if sys.platform.startswith("win") else ["mvn"]
    candidates: list[str] = []
    for env_name in ("MAVEN_HOME", "M2_HOME"):
        home = os.environ.get(env_name)
        if home:
            candidates.extend(existing([Path(home) / "bin" / name for name in names]))
    candidates.extend(existing([ROOT / "mvnw.cmd", ROOT / "mvnw"]))
    if sys.platform.startswith("win"):
        candidates.extend(str(path) for path in Path("C:/bin").glob("apache-maven-*/bin/mvn.cmd"))
    candidates.extend(name for name in ("mvn", "mvn.cmd") if shutil.which(name))
    return candidates


def command_path(name: str) -> str:
    if name == "mvn":
        candidates = maven_candidates()
    else:
        candidates = [name, f"{name}.exe", f"{name}.cmd"] if sys.platform.startswith("win") else [name]
    for candidate in candidates:
        path = shutil.which(candidate) if not Path(candidate).is_absolute() else candidate
        if path:
            return str(path)
    raise SystemExit(f"Required command not found on PATH: {name}")


def run(*args: str, capture: bool = False) -> str:
    command = command_path(args[0])
    command_args = (command, *args[1:])
    if sys.platform.startswith("win") and command.lower().endswith((".cmd", ".bat")):
        command_args = ("cmd.exe", "/c", *command_args)
    result = subprocess.run(
        command_args,
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
    pom = ElementTree.parse(ROOT / "pom.xml")
    version = pom.getroot().findtext("{http://maven.apache.org/POM/4.0.0}version", "").strip()
    if not version or version.startswith("${"):
        raise SystemExit("Could not read Maven project.version")
    return version


def main() -> None:
    require_clean_worktree()
    version = maven_version()
    tag = f"v{version}"
    local_jar = ROOT / "target" / "json-editor.jar"

    run("mvn", "-q", "clean", "package")
    if not local_jar.exists():
        raise SystemExit(f"Expected local JAR not found: {local_jar}")

    require_clean_worktree()
    run("git", "tag", tag)
    run("git", "push", "origin", tag)
    run(
        "gh",
        "release",
        "create",
        tag,
        str(local_jar),
        "--title",
        tag,
        "--notes-file",
        "CHANGELOG.md",
    )


if __name__ == "__main__":
    main()
