#!/usr/bin/env python3
"""Writes a readable result summary to the GitHub Actions job summary page.

Usage:
    summarize.py build <log-file> <outcome>
    summarize.py tests <log-file> <outcome>

<outcome> is the GitHub step outcome: success, failure, cancelled or skipped.
Compiler errors are also printed as workflow annotations, so they show up
next to the offending file and line on the run page.
"""
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

WORKSPACE = os.environ.get("GITHUB_WORKSPACE", "").rstrip("/")
SUMMARY_FILE = os.environ.get("GITHUB_STEP_SUMMARY")
MAX_ANNOTATIONS = 10
MAX_LINES = 40


def clean(text):
    """Shortens absolute runner paths so messages are easier to read."""
    text = text.replace("file://", "")
    if len(WORKSPACE) > 1:
        text = text.replace(WORKSPACE + "/", "")
    return text


def read_lines(path):
    if not os.path.exists(path):
        return []
    with open(path, encoding="utf-8", errors="replace") as handle:
        return handle.read().splitlines()


def code_block(lines, limit=MAX_LINES):
    shown = [clean(line) for line in lines[:limit]]
    extra = len(lines) - len(shown)
    body = "\n".join(shown)
    if extra > 0:
        body += f"\n... and {extra} more"
    return "```\n" + body + "\n```"


def parse_error(line):
    """Returns (path, line_number, message) for a Kotlin or KSP error line, else None."""
    match = re.match(r"^e: (?:file://)?(.+?\.kt):(\d+):(\d+)\s+(.*)$", line)
    if match:
        return match.group(1), match.group(2), match.group(4)
    match = re.match(r"^e: \[ksp\] (.+?\.kt):(\d+): (.*)$", line)
    if match:
        return match.group(1), match.group(2), match.group(3)
    return None


def emit_annotations(error_lines):
    shown = 0
    for line in error_lines:
        if shown >= MAX_ANNOTATIONS:
            break
        parsed = parse_error(line)
        if not parsed:
            continue
        path, line_number, message = parsed
        message = message.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
        print(f"::error file={clean(path)},line={line_number}::{message}")
        shown += 1


# Messages of the Kotlin compile daemon failing to start. Kotlin falls back to compiling
# without it, so they say nothing about errors in the code.
DAEMON_NOISE = re.compile(
    r"^e: (The daemon has terminated|Failed connecting to the daemon|Daemon compilation failed)"
)


def failure_section(lines):
    kotlin_errors = [
        line for line in lines
        if line.startswith("e: ") and not DAEMON_NOISE.match(line)
    ]
    other_errors = [
        line for line in lines
        if re.search(r"\berror:", line) and not line.startswith("e: ")
    ]
    failed_tasks = []
    for line in lines:
        match = re.match(r"^> Task (\S+) FAILED", line)
        if match:
            failed_tasks.append(match.group(1))

    what_went_wrong = []
    if "* What went wrong:" in lines:
        index = lines.index("* What went wrong:") + 1
        while index < len(lines) and not lines[index].startswith("* "):
            what_went_wrong.append(lines[index])
            index += 1

    emit_annotations(kotlin_errors)

    out = []
    if failed_tasks:
        unique = list(dict.fromkeys(failed_tasks))
        out.append("**Failed task:** " + ", ".join(f"`{task}`" for task in unique))
    if kotlin_errors:
        out += ["", f"### Kotlin / KSP errors ({len(kotlin_errors)})", code_block(kotlin_errors)]
    if other_errors:
        out += ["", "### Other compiler errors", code_block(other_errors)]
    if what_went_wrong:
        out += ["", "### What went wrong", code_block(what_went_wrong)]
    if not (kotlin_errors or other_errors or what_went_wrong):
        out += ["", "### Last lines of the log", code_block(lines[-MAX_LINES:])]
    out += ["", "The full log is in the **build-log** / **test-reports** artifact at the bottom of this page."]
    return out


def collect_results(patterns):
    """Reads JUnit XML files. Returns (total, failed, skipped, [(test name, message)])."""
    total = failed = skipped = 0
    failures = []
    files = []
    for pattern in patterns:
        files += glob.glob(pattern, recursive=True)
    for path in files:
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        for case in root.iter("testcase"):
            total += 1
            if case.find("skipped") is not None:
                skipped += 1
            problem = case.find("failure")
            if problem is None:
                problem = case.find("error")
            if problem is not None:
                failed += 1
                text = (problem.get("message") or problem.text or "").strip().splitlines()
                name = f'{case.get("classname", "")}.{case.get("name", "")}'
                failures.append((name, text[0] if text else ""))
    return total, failed, skipped, failures


def failure_table(failures):
    out = ["| Test | Message |", "|---|---|"]
    for name, message in failures:
        safe = message.replace("|", "\\|")[:200]
        out.append(f"| `{name}` | {safe} |")
    return out


UNIT_RESULTS = ["app/build/test-results/testDebugUnitTest/*.xml"]
DEVICE_RESULTS = ["app/build/outputs/androidTest-results/connected/**/*.xml"]


def summarize_build(lines, outcome):
    unit_total, unit_failed, _, unit_failures = collect_results(UNIT_RESULTS)

    if outcome == "success":
        out = ["## ✅ Build successful", ""]
        for apk in sorted(glob.glob("app/build/outputs/apk/debug/*.apk")):
            size_mb = os.path.getsize(apk) / (1024 * 1024)
            out.append(f"- `{os.path.basename(apk)}` ({size_mb:.1f} MB)")
        if unit_total:
            out.append(f"- Unit tests: {unit_total - unit_failed} passed")
        out += ["", "Download the APK from the **Artifacts** section at the bottom of this page."]
        return out

    out = ["## ❌ Build failed", ""]
    if unit_failed:
        out += [f"### Unit test failures ({unit_failed} of {unit_total})"] + failure_table(unit_failures) + [""]
    return out + failure_section(lines)


def summarize_tests(lines, outcome):
    total, failed, skipped, failures = collect_results(DEVICE_RESULTS)

    if total == 0:
        if outcome == "success":
            return ["## ⚠️ No test results found"]
        return ["## ❌ Tests did not run", ""] + failure_section(lines)

    passed = total - failed - skipped
    icon = "✅" if failed == 0 and outcome == "success" else "❌"
    out = [f"## {icon} Instrumented tests: {passed} passed, {failed} failed, {skipped} skipped", ""]
    if failures:
        out += failure_table(failures)
    elif outcome != "success":
        out += failure_section(lines)
    return out


def main():
    mode, log_path, outcome = sys.argv[1:4]
    lines = read_lines(log_path)
    out = summarize_build(lines, outcome) if mode == "build" else summarize_tests(lines, outcome)
    text = "\n".join(out) + "\n"
    print(text)
    if SUMMARY_FILE:
        with open(SUMMARY_FILE, "a", encoding="utf-8") as handle:
            handle.write(text)


if __name__ == "__main__":
    main()
