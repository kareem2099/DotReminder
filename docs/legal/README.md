# DotReminder terms of use

The canonical terms are maintained here in five languages. Each document has
the same ten sections: provider/scope, app functions, notifications, accounts,
optional AI, privacy/advertising, account deletion, service changes,
open-source licensing/mandatory rights, and changes/contact.

| Language | Source | Public HTML route |
| --- | --- | --- |
| English | [terms.en.md](terms.en.md) | `/en/dotreminder/terms` |
| العربية | [terms.ar.md](terms.ar.md) | `/ar/dotreminder/terms` |
| Deutsch | [terms.de.md](terms.de.md) | `/de/dotreminder/terms` |
| Français | [terms.fr.md](terms.fr.md) | `/fr/dotreminder/terms` |
| Русский | [terms.ru.md](terms.ru.md) | `/ru/dotreminder/terms` |

The routes are hosted by the separate DotSuite website. The documents do not
replace the [privacy policy](https://dotsuite.dev/en/privacy#dotreminder) or
[account-deletion request](https://dotsuite.dev/en/privacy#delete-dotreminder).

## Website synchronization

After changing the documents, export the bundle used by DotSuite:

```sh
python3 scripts/export-terms.py --output /path/to/dotsuite/src/content/dotreminder-terms.json
```

The exporter requires all five complete ten-section documents and copies their
text unchanged. DotSuite bundles the JSON in its server-rendered public page;
visitors do not need a GitHub connection or a DotSuite account to read it.
Publish both repositories through their normal GitHub workflows. Website
updates do not change an already signed Android bundle.

Keep the translations aligned when changing obligations or operational
commitments. Do not add blanket liability exclusions, promise guaranteed alarm
delivery or describe deletion as automatic. The 30-day deletion commitment
starts after account-ownership verification, as explained in the privacy policy.

Publishing a page is distinct from obtaining any required acceptance from
existing app users. These documents preserve rights granted by Apache 2.0 and
mandatory consumer rights; they do not certify compliance in every country.
