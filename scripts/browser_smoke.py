"""Chromium UI smoke check; invoked by integration_smoke.py --browser.

Uses real gateway/backend requests. Fixtures are disposable and traces may
contain their temporary credentials; never point this at a shared deployment.
"""
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import secrets
from threading import Thread

from playwright.sync_api import sync_playwright, expect


def run(root, report_dir):
    handler = partial(SimpleHTTPRequestHandler, directory=str(root))
    server = ThreadingHTTPServer(("127.0.0.1", 0), handler)
    thread = Thread(target=server.serve_forever, daemon=True)
    thread.start()
    url = f"http://127.0.0.1:{server.server_port}/collab-editor.html"
    try:
        with sync_playwright() as playwright:
            browser = playwright.chromium.launch()
            context = browser.new_context()
            context.tracing.start(screenshots=True, snapshots=True, sources=True)
            page = context.new_page()
            page.set_default_timeout(15000)
            errors, dialogs = [], []
            page.on("pageerror", lambda error: errors.append(str(error)))

            hostile_title = 'Browser fixture <img src=x onerror="window.titleExecuted=true"> & "quotes"'
            hostile_description = 'Browser snapshot <svg onload="window.descriptionExecuted=true"></svg> & text'

            def assert_literal_fields():
                expect(page.locator("#documentsList .doc-title")).to_have_text(hostile_title)
                expect(page.locator("#allDocsList .doc-title")).to_have_text(hostile_title)
                expect(page.locator("#currentDocInfo strong")).to_have_text(hostile_title)
                expect(page.locator("#documentsList img, #allDocsList img, #currentDocInfo img, #versionsList svg")).to_have_count(0)
                if page.evaluate("Boolean(window.titleExecuted || window.descriptionExecuted)"):
                    raise RuntimeError("Saved text executed as markup")

            def dialog_handler(dialog):
                dialogs.append(dialog.message)
                if dialog.type == "prompt":
                    dialog.accept(hostile_title if "title" in dialog.message else hostile_description)
                else:
                    dialog.accept()

            page.on("dialog", dialog_handler)

            def register_and_login(username):
                password = secrets.token_urlsafe(24)
                page.goto(url)
                page.locator("#loginForm .link").click()
                page.locator("#regFullName").fill("Browser Test")
                page.locator("#regEmail").fill(username + "@example.invalid")
                page.locator("#regUsername").fill(username)
                page.locator("#regPassword").fill(password)
                page.get_by_role("button", name="Create Account", exact=True).click()
                expect(page.locator("#loginForm")).to_be_visible()
                expect(page.locator("#message")).to_have_text("Account created! Please login.")
                page.locator("#loginUsername").fill(username)
                page.locator("#loginPassword").fill(password)
                page.get_by_role("button", name="Login", exact=True).click()
                expect(page.locator("#mainApp")).to_be_visible()
                expect(page.locator("#userUsername")).to_have_text("@" + username)
                expect(page.locator("#documentsList")).to_contain_text("No documents yet")

            try:
                register_and_login("browser_owner")
                page.get_by_role("button", name="+ New Document", exact=True).click()
                expect(page.locator("#currentDocInfo")).to_contain_text("Browser fixture")
                expect(page.locator("#docVisibility")).to_have_value("false")
                expect(page.locator("#documentsList .document-item")).to_have_count(1)
                page.locator("#editor").fill("Saved by the browser")
                page.get_by_role("button", name="💾 Save", exact=True).click()
                expect(page.get_by_role("button", name="✓ Saved", exact=True)).to_be_visible()
                page.locator("#editor").fill("Unsaved change")
                page.locator("#documentsList .document-item").click()
                expect(page.locator("#editor")).to_have_value("Saved by the browser")
                page.get_by_role("button", name="📌 Version", exact=True).click()
                expect(page.locator("#statVersions")).to_have_text("1")
                page.locator(".tab").filter(has_text="Versions").click()
                expect(page.locator("#versionsTab")).to_be_visible()
                expect(page.locator("#versionsList .version-meta").first).to_have_text(hostile_description)
                assert_literal_fields()
                page.locator("#editor").fill("Another unsaved change")
                page.locator("#versionsList .version-item").click()
                expect(page.locator("#editor")).to_have_value("Saved by the browser")
                page.screenshot(path=str(report_dir / "browser-owner.png"), full_page=True)
                document_id = page.evaluate("currentDocId")

                # Simulate another owner session saving the revision this editor loaded.
                external_status = page.evaluate("""async id => {
                    const response = await fetch(`${API_BASE}/documents/${id}`, {
                        method: 'PUT',
                        headers: {'Content-Type': 'application/json', Authorization: `Bearer ${currentUser.token}`},
                        body: JSON.stringify({content: 'Newer session content', revision: currentDocRevision})
                    });
                    return response.status;
                }""", document_id)
                if external_status != 200:
                    raise RuntimeError(f"Competing save failed: {external_status}")
                page.locator("#editor").fill("Unsaved local draft")
                page.get_by_role("button", name="💾 Save", exact=True).click()
                expect(page.get_by_role("button", name="💾 Save", exact=True)).to_be_enabled()
                expect(page.locator("#editor")).to_have_value("Unsaved local draft")
                if not any(message.startswith("Save conflict:") for message in dialogs):
                    raise RuntimeError("Stale save did not display recovery guidance")
                page.locator("#documentsList .document-item").click()
                expect(page.locator("#editor")).to_have_value("Newer session content")
                expect(page.locator("#versionsList .version-meta").first).to_have_text(hostile_description)
                assert_literal_fields()

                # Register/login through the UI as another account, then make a
                # browser-origin request: this exercises CORS, not APIRequestContext.
                register_and_login("browser_other")
                statuses = page.evaluate("""async id => {
                    const headers = {Authorization: `Bearer ${currentUser.token}`};
                    const document = await fetch(`${API_BASE}/documents/${id}`, {headers});
                    const history = await fetch(`${API_BASE}/versions/history/${id}`, {headers});
                    return [document.status, history.status];
                }""", document_id)
                if statuses != [403, 403]:
                    raise RuntimeError(f"Other account access was not denied: {statuses}")
                if errors or any(message.startswith("Error") for message in dialogs):
                    raise RuntimeError(f"Browser errors: {errors}; dialogs: {dialogs}")
                print("PASS: Chromium registration, login, create/save/reload, snapshot load, literal saved text and cross-account denial")
            except Exception:
                page.screenshot(path=str(report_dir / "browser-failure.png"), full_page=True)
                raise
            finally:
                context.tracing.stop(path=str(report_dir / "browser-trace.zip"))
                context.close()
                browser.close()
    finally:
        server.shutdown()
        server.server_close()
        thread.join(timeout=5)
