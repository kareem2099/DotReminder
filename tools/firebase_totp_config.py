#!/usr/bin/env python3
"""Inspect or enable server-enforced TOTP MFA after the Identity Platform upgrade.

Run in Google Cloud Shell or an environment with an authorized gcloud session.
No credentials or user enrollment secrets are printed or stored by this script.
The default command is read-only. --enable changes only MFA provider configs.
"""
import argparse
import json
import subprocess
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", required=True)
    parser.add_argument("--enable", action="store_true")
    args = parser.parse_args()
    try:
        result = subprocess.run(["gcloud", "auth", "print-access-token"], capture_output=True, text=True, check=True)
    except (FileNotFoundError, subprocess.CalledProcessError):
        sys.exit("Open Google Cloud Shell with an account authorized to manage this project's Authentication config.")
    token = result.stdout.strip()
    endpoint = "https://identitytoolkit.googleapis.com/admin/v2/projects/" + quote(args.project, safe="") + "/config"
    headers = {"Authorization": "Bearer " + token, "X-Goog-User-Project": args.project, "Content-Type": "application/json"}

    def request(method="GET", payload=None, query=""):
        data = None if payload is None else json.dumps(payload).encode()
        try:
            with urlopen(Request(endpoint + query, data=data, headers=headers, method=method), timeout=30) as response:
                return json.load(response)
        except HTTPError as error:
            sys.exit(f"Authentication config request failed (HTTP {error.code}). Check project permissions and the Identity Platform upgrade. No success is assumed; re-run the read-only check.")
        except URLError:
            sys.exit("Could not reach the Authentication API. Re-run the read-only check before retrying a change.")

    def totp(config):
        return next((p for p in config.get("mfa", {}).get("providerConfigs", []) if "totpProviderConfig" in p), None)

    config = request()
    current = totp(config)
    print("Project:", args.project)
    print("TOTP:", (current or {}).get("state", "NOT_CONFIGURED"))
    if not args.enable:
        print("Read-only check. After reviewing the Identity Platform pricing upgrade, use --enable to enable TOTP.")
        return
    if current and current.get("state") == "ENABLED":
        print("TOTP is already enabled. Existing provider configuration preserved.")
        return
    providers = list(config.get("mfa", {}).get("providerConfigs", []))
    enabled = {**(current or {}), "state": "ENABLED", "totpProviderConfig": {"adjacentIntervals": 1}}
    providers = [enabled if "totpProviderConfig" in provider else provider for provider in providers]
    if current is None:
        providers.append(enabled)
    # Preserve SMS, other MFA settings and the rest of the project configuration.
    request("PATCH", {"mfa": {"providerConfigs": providers}}, "?updateMask=mfa.providerConfigs")
    actual = totp(request())
    if not actual or actual.get("state") != "ENABLED":
        sys.exit("The server did not confirm TOTP activation. Do not declare MFA ready.")
    print("Verified: server-side TOTP MFA is ENABLED. No user factors were enrolled or removed.")


if __name__ == "__main__":
    main()
