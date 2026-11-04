"""Run only through the demo-project Firebase Auth/Firestore emulators; no real accounts."""
import json
import os
import unittest
from urllib.error import HTTPError
from urllib.request import Request, urlopen

PROJECT = "demo-dotreminder-auth"
FIRESTORE = os.environ.get("FIRESTORE_EMULATOR_HOST", "")
AUTH = os.environ.get("FIREBASE_AUTH_EMULATOR_HOST", "")
if FIRESTORE != "127.0.0.1:8087" or AUTH != "127.0.0.1:9097":
    raise SystemExit("Only the isolated local auth-review emulators are allowed")


def request(url, method="GET", body=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = None if body is None else json.dumps(body).encode()
    try:
        with urlopen(Request(url, data=data, method=method, headers=headers), timeout=15) as response:
            return response.status, json.loads(response.read() or "{}")
    except HTTPError as error:
        return error.code, {}


def user():
    status, account = request(
        f"http://{AUTH}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key",
        "POST", {"returnSecureToken": True})
    if status != 200:
        raise RuntimeError("Could not create emulator-only identity")
    return account["localId"], account["idToken"]


class SessionRulesTest(unittest.TestCase):
    def setUp(self):
        self.uid, self.token = user()
        _, self.other_token = user()
        self.root = f"http://{FIRESTORE}/v1/projects/{PROJECT}/databases/(default)/documents"
        self.path = f"/users/{self.uid}/sessions/test-session"
        self.fields = {"fields": {"deviceName": {"stringValue": "Emulator review"}}}
        status, _ = self.call("PATCH", token=self.token)
        self.assertEqual(200, status, "Owner must be able to create their session")

    def call(self, method="GET", token=None, path=None):
        return request(self.root + (path or self.path), method,
                       self.fields if method == "PATCH" else None, token)

    def test_owner_can_read(self):
        self.assertEqual(200, self.call(token=self.token)[0])

    def test_owner_can_list_sessions(self):
        self.assertEqual(200, self.call(token=self.token, path=f"/users/{self.uid}/sessions")[0])

    def test_owner_can_update(self):
        self.assertEqual(200, self.call("PATCH", token=self.token)[0])

    def test_owner_can_delete(self):
        self.assertEqual(200, self.call("DELETE", token=self.token)[0])

    def test_other_user_cannot_read(self):
        self.assertEqual(403, self.call(token=self.other_token)[0])

    def test_other_user_cannot_list(self):
        self.assertEqual(403, self.call(token=self.other_token, path=f"/users/{self.uid}/sessions")[0])

    def test_other_user_cannot_write(self):
        self.assertEqual(403, self.call("PATCH", token=self.other_token)[0])

    def test_other_user_cannot_delete(self):
        self.assertEqual(403, self.call("DELETE", token=self.other_token)[0])

    def test_signed_out_cannot_read(self):
        self.assertEqual(403, self.call()[0])

    def test_signed_out_cannot_write(self):
        self.assertEqual(403, self.call("PATCH")[0])

    def test_signed_out_cannot_delete(self):
        self.assertEqual(403, self.call("DELETE")[0])

    def test_no_access_to_unrelated_user_subcollections(self):
        self.assertEqual(403, self.call("PATCH", token=self.token, path=f"/users/{self.uid}/private/example")[0])


if __name__ == "__main__":
    unittest.main(verbosity=2)
