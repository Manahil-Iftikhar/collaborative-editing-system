"""Run against fresh local JARs; creates disposable fixtures and stops its own services.

Requires Java 17, Python 3.10+, packaged modules, and unused ports 8080-8083.
Uses real HTTP, H2, JWTs and the gateway; no third-party Python dependencies.
"""
import argparse
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import time
from urllib.error import HTTPError, URLError
from urllib.request import Request, build_opener, ProxyHandler

ROOT = Path(__file__).resolve().parents[1]
MODULES = [("user-service", 8081), ("document-service", 8082),
           ("version-service", 8083), ("api-gateway", 8080)]
HTTP = build_opener(ProxyHandler({}))
CHECKS = []


def request(method, path, expected, token=None, body=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    payload = None if body is None else json.dumps(body).encode()
    req = Request("http://localhost:8080/api" + path, payload, headers, method=method)
    try:
        response = HTTP.open(req, timeout=15)
    except HTTPError as exc:
        response = exc
    with response:
        status, raw = response.status, response.read()
    if status != expected:
        raise RuntimeError(f"{method} {path}: expected {expected}, received {status}")
    CHECKS.append(f"{method} {path}: {status}")
    return json.loads(raw) if raw else None


def check(condition, label):
    if not condition:
        raise RuntimeError(label)
    CHECKS.append(label)


def stop(process):
    if process.poll() is None:
        process.terminate()
        try:
            process.wait(timeout=15)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=10)


def wait_ready(process, port):
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f"Service on {port} exited during startup; inspect its log")
        try:
            with HTTP.open(f"http://localhost:{port}/api/readiness-probe", timeout=2):
                return
        except HTTPError:
            return  # An HTTP error also proves the web server has started.
        except (URLError, TimeoutError, OSError):
            time.sleep(0.5)
    raise RuntimeError(f"Service on {port} did not start within 90 seconds")


def check_database_consoles():
    # Probe backend ports directly; checking only the gateway could hide exposure.
    for port, expected in ((8081, 401), (8082, 404), (8083, 404)):
        for path in ("/h2-console", "/h2-console/"):
            try:
                response = HTTP.open(f"http://localhost:{port}{path}", timeout=5)
            except HTTPError as exc:
                response = exc
            with response:
                status = response.status
            if status != expected:
                raise RuntimeError(f"Console on {port}{path}: expected {expected}, received {status}")
            CHECKS.append(f"Database console unavailable on {port}{path}: {status}")


def exercise(users):
    accounts = []
    for name in ("smoke_owner", "smoke_other"):
        password = secrets.token_urlsafe(24)
        account = request("POST", "/users/register", 201, body={
            "username": name, "email": name + "@example.invalid",
            "password": password, "fullName": "Integration Fixture"})
        login = request("POST", "/users/login", 200,
                        body={"username": name, "password": password})
        accounts.append((account["id"], login["token"]))
    (owner_id, owner), (other_id, other) = accounts
    request("GET", "/users/me", 401)
    check(request("GET", "/users/me", 200, owner)["id"] == owner_id, "Authenticated identity matches owner")
    request("GET", "/users/profile/smoke_owner", 403, other)
    request("POST", "/documents", 401, body={"title": "Denied"})

    for public in (False, True):
        document = request("POST", "/documents", 201, owner, {
            "title": "Disposable integration fixture", "content": "initial",
            "ownerId": other_id, "public": public})
        doc_id = document["id"]
        check(document["ownerId"] == owner_id, "Creation ignores forged ownerId")
        check(document["public"] is public, "Document visibility preserved")
        path = f"/documents/{doc_id}"
        request("GET", path, 200 if public else 401)
        request("GET", path, 200 if public else 403, other)
        request("GET", path, 200, owner)
        edit = {"content": "edited", "userId": other_id, "changeType": "UPDATE", "position": 0, "revision": document["revision"]}
        request("PUT", path, 403, other, edit)
        request("PUT", path, 428, owner, {"content": "missing revision"})
        saved = request("PUT", path, 200, owner, edit)
        check(saved["revision"] == document["revision"] + 1, "Save advances revision")
        request("PUT", path, 409, owner, dict(edit, content="stale overwrite"))
        check(request("GET", path, 200, owner)["content"] == "edited", "Stale save preserves current content")
        changes = request("GET", path + "/changes", 200, owner)
        check(len(changes) == 1 and all(c["userId"] == owner_id for c in changes), "Change actor derives from token")
        request("GET", path + "/changes", 403, other)

        version_body = {"documentId": doc_id, "content": "snapshot",
                        "userId": other_id, "changeDescription": "Integration fixture"}
        request("POST", "/versions", 401, body=version_body)
        request("POST", "/versions", 403, other, version_body)
        version = request("POST", "/versions", 201, owner, version_body)
        check(version["createdBy"] == owner_id, "Snapshot ignores forged userId")
        number = version["versionNumber"]
        for route in (f"/versions/history/{doc_id}", f"/versions/{doc_id}/{number}",
                      f"/versions/contributions/{doc_id}"):
            request("GET", route, 401)
            request("GET", route, 403, other)
            request("GET", route, 200, owner)
        revert = f"/versions/revert?documentId={doc_id}&versionNumber={number}&userId={other_id}"
        request("POST", revert, 403, other)
        restored = request("POST", revert, 200, owner)
        check(restored["createdBy"] == owner_id and restored["content"] == "snapshot", "Revert creates owner-attributed snapshot")
        check(request("GET", path, 200, owner)["content"] == "edited", "Snapshot revert leaves current document unchanged")

    request("GET", f"/documents/owner/{owner_id}", 403, other)
    check(len(request("GET", f"/documents/owner/{owner_id}", 200, owner)) == 2, "Owner list includes both fixtures")
    request("GET", "/users/me", 401, "invalid-token")
    stop(users)
    request("POST", "/documents", 503, owner, {"title": "Denied during identity outage"})
    request("GET", f"/versions/history/{doc_id}", 503, owner)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--browser', action='store_true', help='Also run the Chromium UI checks')
    args = parser.parse_args()
    processes, logs = [], []
    report_dir = ROOT / "integration-results"
    report_dir.mkdir(exist_ok=True)
    outcome = {"status": "failed", "checks": CHECKS}
    try:
        # Refuse existing services so this test cannot modify a developer's running data.
        for _, port in MODULES:
            with socket.socket() as probe:
                if probe.connect_ex(("127.0.0.1", port)) == 0:
                    raise RuntimeError(f"Port {port} is in use; stop existing services first")
        env = dict(os.environ, JWT_SECRET=secrets.token_hex(32),
                   USER_SERVICE_URL="http://localhost:8081", DOCUMENT_SERVICE_URL="http://localhost:8082")
        for module, port in MODULES:
            jars = list((ROOT / module / "target").glob("*.jar"))
            if len(jars) != 1:
                raise RuntimeError(f"Package {module} first; expected one executable JAR")
            log = (report_dir / f"{module}.log").open("w")
            logs.append(log)
            process = subprocess.Popen(["java", "-Xmx256m", "-jar", str(jars[0])],
                                       cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
            processes.append(process)
            wait_ready(process, port)
        check_database_consoles()
        if args.browser:
            from browser_smoke import run
            run(ROOT, report_dir)
            CHECKS.append('Chromium UI workflow and cross-account denial passed')
        exercise(processes[0])
        outcome["status"] = "passed"
        print(f"PASS: {len(CHECKS)} HTTP and data checks through the gateway")
    except Exception as exc:
        outcome["error"] = str(exc)
        raise
    finally:
        for process in reversed(processes):
            stop(process)
        for log in logs:
            log.close()
        (report_dir / "summary.json").write_text(json.dumps(outcome, indent=2) + "\n")


if __name__ == "__main__":
    main()
