# DIRU-NA-AUDIT-003 — Nokia MIDlet Identity and Platform Metadata

**Status:** Static metadata audit completed; original package provenance not independently authenticated  
**Audit input:** `kubikaugustyn/DiamondRushSource`  
**Reviewed source revision:** `215c71d04057d66a1b08419b1a4b1043ae3e0881`  
**Development target:** `yogiags56/Project-DIRU-NA` only  
**Scope:** MIDlet manifest identity, Nokia-specific deployment attributes, Nokia API compatibility stubs, and limits of the available evidence.

> This audit is read-only. No source, binary, manifest, or asset was copied from the candidate repository into Project DIRU-NA. Static metadata is evidence of the package identity represented by the archived source tree; it is not cryptographic proof that the tree is an untouched official Nokia distribution.

## 1. Manifest evidence

Both files were inspected at the pinned candidate revision:

- `src/main/resources/META-INF/MANIFEST.MF`
- `target/classes/META-INF/MANIFEST.MF`

Both contain the same relevant attributes:

| Attribute | Value | Interpretation |
|---|---|---|
| `MIDlet-Name` | `Diamond Rush` | MIDlet suite display name |
| `MIDlet-Vendor` | `Nokia` | Package metadata explicitly identifies Nokia as vendor |
| `MIDlet-Version` | `1.1.7` | Version represented by this package |
| `MIDlet-1` | `Diamond Rush, /icon.png, GloftDIRU` | MIDlet entry point is `GloftDIRU` |
| `MicroEdition-Configuration` | `CLDC-1.0` | Java ME configuration |
| `MicroEdition-Profile` | `MIDP-2.0` | Java ME profile |
| `Nokia-MIDlet-Category` | `Game` | Nokia-specific deployment/category metadata |
| `Nokia-Platform` | `*` | Nokia platform metadata is present; wildcard value should not be over-interpreted as a particular handset model |
| `MIDlet-Data-Size` | `1200` | Declared MIDlet data-size hint |

The two manifest copies agree byte-for-byte in their visible text. The resource manifest is part of the candidate source tree; the target manifest is a compiled-output copy retained in that same tree.

## 2. MIDlet entry point

The candidate's `src/main/java/GloftDIRU.java` declares:

- `public final class GloftDIRU extends MIDlet`
- Constructor reads `MIDlet-Version` using `getAppProperty`
- Creates the game canvas instance
- `startApp()` obtains the Java ME display and sets the game canvas as current
- `pauseApp()` and `destroyApp()` forward lifecycle events to game logic

This is consistent with the manifest's `MIDlet-1` entry point. It confirms a Java ME MIDlet package, but the lifecycle class alone does not identify the handset manufacturer.

## 3. Nokia API namespace findings

The candidate tree contains Nokia namespace compatibility classes, including:

- `com/nokia/mid/ui/FullCanvas.java`
- `com/nokia/mid/ui/DeviceControl.java`
- `com/nokia/mid/ui/DirectGraphics.java`
- `com/nokia/mid/sound/Sound.java`
- `com/nokia/mid/m3d/M3D.java`

Important qualification: these files are compatibility implementations/stubs in the FreeJ2ME-derived source tree, not the original Nokia handset API binaries. For example, `DeviceControl` methods are empty, and `Sound` methods return placeholders. Their presence shows that the source tree includes Nokia API namespace support, but does not by itself prove which Nokia-specific APIs the original commercial build invoked at runtime.

The tree also contains Siemens API namespace compatibility classes. This may reflect a broad compatibility/decompilation environment; it does not negate the explicit Nokia vendor metadata, but it means namespace inventory must not be mistaken for a precise original-device target list.

## 4. Classification

**Project classification for the audited candidate: Nokia-identified Diamond Rush MIDlet package (version 1.1.7), with strong static metadata evidence.**

The classification is supported by the explicit `MIDlet-Vendor: Nokia` field and Nokia-specific manifest attributes, not merely by the repository description or generic J2ME compatibility.

Use this wording in DIRU-NA documentation:

> **Gameloft Diamond Rush — Nokia-identified J2ME MIDlet package (manifest version 1.1.7).**

Avoid these stronger claims until independently verified:

- “Official Nokia original JAR”
- “Nokia-exclusive release”
- “Built specifically for [a named Nokia handset/model]”
- “Untouched commercial distribution package”

The candidate repository describes the game as Gameloft's 2006 Diamond Rush for Nokia mobile devices. The manifest independently names Nokia as MIDlet vendor. These sources together make the Nokia-package classification well-supported, while package authenticity and original distribution provenance remain unverified.

## 5. What would close the provenance gap

A stronger authenticity determination requires one or more independently sourced original artifacts:

1. Original JAR and matching JAD from a documented Nokia/Ovi/operator distribution or a preserved handset backup.
2. SHA-256 hashes and acquisition provenance for both artifacts.
3. Comparison of the original JAR's `META-INF/MANIFEST.MF` with the candidate manifest.
4. Comparison of MIDlet version, vendor, entry point, resource inventory, and class/resource hashes.
5. If signed, inspection of signature metadata and certificate chain. A decompiled source tree is not a substitute for the original signed package.

Do not modify the candidate repository or attempt to reconstruct a purported official JAR from the decompiled tree as proof of authenticity.

## 6. Project implications

- Keep the 240×320 portrait logical viewport as the DIRU-NA initial implementation baseline already decided by the project. Nokia manifest identity does not independently prove the original handset's screen orientation or physical resolution.
- Preserve the package version `1.1.7` as an observed source metadata value, not as a new DIRU-NA app version.
- Continue to treat the candidate source and proprietary game resources as read-only audit input pending provenance/licensing review.
- Do not embed FreeJ2ME or its Nokia API stubs in the Native Android runtime.
- Reimplement only verified behavior using Android-native modules after rights and technical review.
- Do not import code, build files, assets, generated output, or binaries from the archived Native Android project.

## 7. References inspected

- Candidate repository: https://github.com/kubikaugustyn/DiamondRushSource
- Pinned candidate revision: https://github.com/kubikaugustyn/DiamondRushSource/tree/215c71d04057d66a1b08419b1a4b1043ae3e0881
- Manifest: https://github.com/kubikaugustyn/DiamondRushSource/blob/215c71d04057d66a1b08419b1a4b1043ae3e0881/src/main/resources/META-INF/MANIFEST.MF
- MIDlet entry point: https://github.com/kubikaugustyn/DiamondRushSource/blob/215c71d04057d66a1b08419b1a4b1043ae3e0881/src/main/java/GloftDIRU.java
- Nokia MIDlet attributes reference: https://github.com/Nokia64/J2ME-phone-quirks/blob/main/vendor-specific-MIDlet-attributes.md

## 8. Validation status

| Check | Result |
|---|---|
| Source revision pinned | Complete |
| Resource manifest inspected | Complete |
| Compiled-output manifest inspected | Complete |
| Manifest identity fields compared | Complete; visible text matches |
| MIDlet entry point inspected | Complete |
| Nokia API compatibility namespace inventoried | Partial; namespace presence confirmed, runtime call graph not fully mapped |
| Original JAR/JAD independently obtained | Not available in this audit |
| Package signature/provenance authenticated | Not performed |
| Nokia handset/model-specific behavior established | Not established |

No build, APK, handset test, or runtime validation was performed as part of this metadata audit.


## 9. Additional interpretation guardrails

The Nokia-specific attribute reference was consulted to avoid over-reading the manifest:

- `Nokia-MIDlet-Category: Game` is a recognized Nokia attribute used to categorize the MIDlet in the device's application/game menu. This is meaningful Nokia deployment metadata.
- `Nokia-Platform: *` is present in this candidate manifest, but the reviewed attribute reference does not define this field/value. Treat it as an opaque vendor/platform marker; do not infer a handset family, model, firmware, or screen profile from the wildcard.
- The manifest does **not** contain `Nokia-MIDlet-App-Orientation`, `Nokia-MIDlet-Original-Display-Size`, or `Nokia-MIDlet-Target-Display-Size`. Therefore it does not establish portrait/landscape locking or a physical display resolution.
- `MIDlet-Vendor: Nokia` is strong evidence that this particular package metadata labels Nokia as vendor. It does not independently authenticate the package, prove Nokia authored the game, or establish whether Nokia was publisher, distributor, carrier partner, or repackager. Keep the separate game-credit question open until original distribution evidence is obtained.

Reference for Nokia MIDlet attributes: https://github.com/Nokia64/J2ME-phone-quirks/blob/main/vendor-specific-MIDlet-attributes.md
