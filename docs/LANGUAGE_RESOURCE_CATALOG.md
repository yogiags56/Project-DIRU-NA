# Language Resource Catalog — DIRU-NA Audit 001

## Scope and preservation requirement

The candidate source repository `kubikaugustyn/DiamondRushSource` contains 15 language-related resource files under `src/main/resources`: 14 locale-specific resources and one fallback resource.

**Project requirement:** preserve every language resource found in the candidate. Do not silently drop a locale, replace it with a translation from another locale, rename locale identifiers, or convert/normalize the binary payload during initial preservation.

The files are binary-encoded resources, not ordinary UTF-8 text. The GitHub text-file fetch returned a UTF-8 decoding error for at least one language resource. Therefore this catalog records filenames and loader behavior only; it does not claim that translation strings, entry counts, completeness, or exact binary checksums have been audited.

## Locale inventory

| Resource file | Locale identifier | Language |
|---|---|---|
| `lang.bs-BA` | `bs-BA` | Bosnian (Bosnia and Herzegovina) |
| `lang.cs-CZ` | `cs-CZ` | Czech (Czechia) |
| `lang.de` | `de` | German |
| `lang.fr` | `fr` | French |
| `lang.hr-HR` | `hr-HR` | Croatian (Croatia) |
| `lang.hu-HU` | `hu-HU` | Hungarian (Hungary) |
| `lang.mk-MK` | `mk-MK` | Macedonian (North Macedonia) |
| `lang.pl-PL` | `pl-PL` | Polish (Poland) |
| `lang.ro-RO` | `ro-RO` | Romanian (Romania) |
| `lang.ru-RU` | `ru-RU` | Russian (Russia) |
| `lang.sk-SK` | `sk-SK` | Slovak (Slovakia) |
| `lang.sl-SI` | `sl-SI` | Slovenian (Slovenia) |
| `lang.sq` | `sq` | Albanian |
| `lang.sr-YU` | `sr-YU` | Serbian (legacy Yugoslavia locale identifier) |
| `lang.xx` | fallback | Unknown/default fallback resource; not counted as a selectable language |

**Current inventory: 14 locale-specific resources + 1 fallback resource.**

## Existing loader behavior

Source: `src/main/java/d.java`.

The existing localization loader:

1. Reads the device locale from `System.getProperty("microedition.locale")`.
2. Attempts to open `/lang.<locale>`.
3. If that resource is unavailable, attempts `/lang.xx`.
4. Reads localized strings by index from a binary data stream.

This indicates that language selection is currently inherited from the device locale in the candidate implementation. It is not yet a user-selectable in-game language menu.

## Native Android language-selection requirement

The Native Android implementation must provide an explicit language selection feature while retaining all 14 discovered locale resources.

Requirements:

- Show every available locale in the language menu.
- Keep the fallback resource as a fallback only; do not present `xx` as a language choice.
- Persist the user's selected locale independently of the Android system locale.
- On first launch, choose a deterministic default: use the Android/device locale if an exact supported locale exists; otherwise use the project's documented default locale, with fallback text available for missing entries.
- Allow the user to change language without losing game progress.
- Reload localized strings safely when the selection changes.
- Preserve the original resource-to-string index mapping unless a verified migration is documented.
- Keep source resource names and locale identifiers in a manifest/catalog so future resource additions cannot accidentally remove an existing language.
- Do not translate missing strings automatically or substitute another language silently without recording the fallback behavior.

## Preservation and validation workflow

Before copying any candidate resource into the new repository:

1. Review source provenance and redistribution rights.
2. Retrieve the original resource bytes without text decoding.
3. Record SHA-256, byte length, and source revision for each resource.
4. Store each approved original resource unchanged in the isolated DIRU-NA resource tree.
5. Build a parser/validator for the binary string table.
6. Compare entry counts and indexed strings across all locale resources.
7. Report missing, empty, malformed, and unusually long entries per locale.
8. Add regression tests proving all locale IDs remain available.

No original language payload has been copied into Project DIRU-NA as part of this documentation update.

## Source references

- Candidate language resource directory: https://github.com/kubikaugustyn/DiamondRushSource/tree/main/src/main/resources
- Candidate localization loader: https://github.com/kubikaugustyn/DiamondRushSource/blob/main/src/main/java/d.java
