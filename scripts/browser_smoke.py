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

            def dialog_handler(dialog):
                dialogs.append(dialog.message)
                if dialog.type == "prompt":
                    dialog.accept("Browser fixture" if "title" in dialog.message else "Browser snapshot")
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
                expect(page.locator("#versionsList")).to_contain_text("Browser snapshot")
                page.locator("#editor").fill("Another unsaved change")
                page.locator("#versionsList .version-item").click()
                expect(page.locator("#editor")).to_have_value("Saved by the browser")
                page.screenshot(path=str(report_dir / "browser-owner.png"), full_page=True)
                document_id = page.evaluate("currentDocId")

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
                print("PASS: Chromium registration, login, create/save/reload, snapshot load and cross-account denial")
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
