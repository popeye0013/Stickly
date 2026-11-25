<a id="stickly"></a>

<p align="center">
  <img src="docs/stickly-banner.svg" alt="Stickly" width="100%" />
</p>

<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=Google+Sans&weight=600&size=22&pause=1000&color=FF7A59&center=true&vCenter=true&width=560&lines=Telegram+stickers%2C+new+home.;Material+3+Expressive+%C2%B7+On-device+token;512%C3%97512+WebP+%C2%B7+static+%26+animated" alt="Typing animation" />
</p>

<p align="center">
  <a href="https://github.com/popeye0013/Stickly/releases/latest">
    <img src="https://img.shields.io/github/v/release/popeye0013/Stickly?style=flat&color=FF7A59&labelColor=2A2A32" alt="Latest release" />
  </a>
  <a href="https://github.com/popeye0013/Stickly/releases">
    <img src="https://img.shields.io/github/downloads/popeye0013/Stickly/total?style=flat&color=FF7A59&labelColor=2A2A32" alt="Total downloads" />
  </a>
  <a href="https://github.com/popeye0013/Stickly/stargazers">
    <img src="https://img.shields.io/github/stars/popeye0013/Stickly?style=flat&color=7C3AED&labelColor=2A2A32" alt="Stars" />
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/popeye0013/Stickly?style=flat&color=7C3AED&labelColor=2A2A32" alt="GPL-3.0 license" />
  </a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat&logo=android&logoColor=white&labelColor=2A2A32" alt="Android 8.0+" />
</p>

<p align="center">
  <a href="https://github.com/popeye0013/Stickly/releases/latest">
    <img src="https://img.shields.io/badge/Download_the_latest_APK-FF7A59?style=flat&logo=android&logoColor=white&labelColor=2A2A32" alt="Download the latest APK" />
  </a>
  <a href="Stickly-v1.0.apk">
    <img src="https://img.shields.io/badge/Direct_APK-v1.0-6750A4?style=flat&logo=android&logoColor=white&labelColor=2A2A32" alt="Direct APK" />
  </a>
</p>

<p align="center">
  <a href="#download">Download</a> ·
  <a href="#how-it-works">How it works</a> ·
  <a href="#features">Features</a> ·
  <a href="#format-rules">Format rules</a> ·
  <a href="#build-from-source">Build</a> ·
  <a href="#tech-stack">Stack</a> ·
  <a href="#faq">FAQ</a>
</p>

---

## <img src="docs/icons/features.svg" width="24" height="24" alt="" />&nbsp;Features

### <img src="docs/icons/discover.svg" width="21" height="21" alt="" />&nbsp;Import

- **Every sticker is imported.** No skips, no placeholders unless a source truly cannot be decoded.
- **Content-aware conversion.** Downloads are detected from their bytes as PNG/WebP, gzipped `TGS`, or WebM, instead of trusting Telegram's metadata.
- **Balanced splits.** Packs larger than 30 stickers become evenly sized WhatsApp packs named `Your Name (1/4)` … `(4/4)`.
- **Fast parallel prep.** Downloads and conversions run concurrently with bounded parallelism.
- **Your name only.** Stickly asks for a pack name; WhatsApp always shows **Stickly** as the publisher.

### <img src="docs/icons/play.svg" width="21" height="21" alt="" />&nbsp;Export

- **WhatsApp-ready WebP.** Static stickers are 512×512 WebP under 100 KB; animated stickers are real animated WebP under 500 KB.
- **Real animation.** `TGS` and video stickers keep their motion; static stickers in an animated pack are converted so packs never mix formats.
- **Valid frame timing.** Every animated frame is longer than 8 ms and the total stays under 10 seconds.
- **Provider flag set.** Animated packs advertise `animated_sticker_pack=1` to WhatsApp.
- **Added badge.** After WhatsApp confirms the pack, **Add to WhatsApp** is replaced by an **Added to WhatsApp** badge.
- **ZIP export.** Save the whole pack to any location using the Android file picker.

### <img src="docs/icons/design-details.svg" width="21" height="21" alt="" />&nbsp;Design &amp; details

- **Material 3 Expressive** — expressive shapes, buttons, loading indicators and `MotionScheme.expressive()`.
- **Themes** — System / Light / Dark, plus optional Material You dynamic colours (off by default).
- **Google Sans Flex** — used at weights 400–900 throughout the app.
- **In-app hints** — dismissible hints that disappear after 3 seconds; no toasts or snackbars.
- **Animated welcome** — a friendly first-launch screen with no tutorial steps.
- **Local profile** — your name stays on this device.

---

## <img src="docs/icons/discover.svg" width="24" height="24" alt="" />&nbsp;How it works

1. **Settings → Telegram bot token.** Telegram's Bot API is the only reliable public way to read a sticker set. A **Guide** button below Save token opens a popup showing how to create a bot with [@BotFather](https://t.me/BotFather). The token stays on your device and is only sent to `api.telegram.org`.
2. **Home → paste a pack link**, for example `t.me/addstickers/DonutTheDog`, then tap **Import pack**. You can also share a `t.me/addstickers/...` link from another app. The URL field clears after a successful import.
3. **Stickly converts everything.** PNG/WebP are re-encoded as static WebP; `TGS` and WebM are converted to animated WebP. If any sticker animates, the whole pack is exported as animated WebP. Generated files are checked before they reach WhatsApp.
4. **Open a pack.** Tap any sticker to view it full screen, or long-press stickers to select up to 25 and create a custom WhatsApp pack. Long-press a pack on Home to delete it.
5. **Add to WhatsApp.** Stickly prepares/repairs the pack, opens WhatsApp's preview, and shows an **Added to WhatsApp** badge once confirmed.

---

## <img src="docs/icons/audio-quality.svg" width="24" height="24" alt="" />&nbsp;Format rules

WhatsApp's sticker contract is strict. Stickly applies these rules automatically:

| Rule | What Stickly does |
|---|---|
| Dimensions | Every sticker is rendered on a transparent 512×512 canvas. |
| Static size | Re-encoded as WebP up to 100 KB, with quality and scaling fallbacks. |
| Animated size | Real animated WebP up to 500 KB, with quality/frame sampling fallbacks. |
| Frame timing | More than 8 ms per frame and no more than 10 seconds total. |
| Tray icon | 96×96 PNG up to 50 KB. |
| Pack size | 3–30 stickers per pack; larger sets are split evenly. |
| No mixing | If one sticker animates, every sticker in that pack becomes animated. |
| Emojis | Telegram emojis are mapped to WhatsApp's supported list; a default is supplied when missing. |
| Identifiers | Pack IDs stay under WhatsApp's 128-character limit. |

---

## <img src="docs/icons/build-from-source.svg" width="24" height="24" alt="" />&nbsp;Download

The signed release APK is committed in this repository as [`Stickly-v1.0.apk`](Stickly-v1.0.apk) and attached to the [latest GitHub release](https://github.com/popeye0013/Stickly/releases/latest).

```text
Stickly-v1.0.apk
SHA-256: 01ce672fbd4130eab369f0d4a1957839a3cd49263729920bf60652d47359659b
```

Install it directly on Android 8.0 or newer. The app is signed with a release key so it can be installed without a separate debug build.

---

## <img src="docs/icons/build-from-source.svg" width="24" height="24" alt="" />&nbsp;Build from source

**Requirements:** JDK 17 and Android SDK 36.

```bash
git clone https://github.com/popeye0013/Stickly.git
cd Stickly
./gradlew assembleRelease
```

The signed APK is written to:

```text
app/build/outputs/apk/release/app-release.apk
```

For a debug build:

```bash
./gradlew assembleDebug
```

`local.properties` must point `sdk.dir` at an Android SDK with platform 36 installed.

---

## <img src="docs/icons/tech-stack.svg" width="24" height="24" alt="" />&nbsp;Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.4 |
| UI | Jetpack Compose, Material 3 Expressive `1.5.0-alpha13` |
| Networking | OkHttp, Telegram Bot API |
| Stickers | `webp-android` / native libwebp, Lottie `TGS`, Coil 3 |
| WhatsApp | Official sticker content provider contract |
| Storage | SharedPreferences + on-device sticker files |
| Images | Android `ImageDecoder`, `BitmapFactory`, custom WebP encoding |
| Build | AGP 8.13.2, Gradle 8.13 |

### <img src="docs/icons/project-layout.svg" width="21" height="21" alt="" />&nbsp;Project layout

```text
app/src/main/java/com/stickly/app/
├── data/        # Models, repository, preferences and theming state
├── telegram/    # Bot API client, file download and pack parsing
├── util/        # WebP, animated WebP, TGS/video and tray conversion
├── whatsapp/    # Content provider and add-pack intent
└── ui/          # Compose screens, components, dialogs and theme
```

---

## <img src="docs/icons/faq.svg" width="24" height="24" alt="" />&nbsp;FAQ

<details>
<summary><strong>Do I need a Telegram bot token?</strong></summary>

Yes. Telegram has no public unauthenticated endpoint for reading sticker sets. Create a free bot with [@BotFather](https://t.me/BotFather), paste the token into Settings, and Stickly will only send it to `api.telegram.org`. The token is stored on your device.

</details>

<details>
<summary><strong>Why does one imported pack become several packs?</strong></summary>

WhatsApp allows 3–30 stickers per pack. Stickly splits larger Telegram sets into balanced packs and names them `Your Name (1/4)`, `(2/4)`, and so on.

</details>

<details>
<summary><strong>Can I mix static and animated stickers?</strong></summary>

No. WhatsApp rejects mixed packs. If any sticker is animated, Stickly converts every sticker in that pack to animated WebP and sets the animated pack flag.

</details>

<details>
<summary><strong>Why does an animated sticker show as an emoji sometimes?</strong></summary>

If a source frame cannot be decoded, Stickly uses an emoji placeholder so the sticker is never dropped and the pack stays importable.

</details>

<details>
<summary><strong>Is anything uploaded?</strong></summary>

No analytics or tracking SDKs. Only the Telegram Bot API calls needed to read the pack and download its files leave your device. Stickers, tokens and preferences stay local.

</details>

<details>
<summary><strong>Is Stickly affiliated with Telegram or WhatsApp?</strong></summary>

No. Stickly is an independent, unofficial project. All trademarks and sticker content belong to their respective owners. Only import packs you have the right to reuse.

</details>

---

## <img src="docs/icons/contributing.svg" width="24" height="24" alt="" />&nbsp;Contributing

Issues and pull requests are welcome.

- Keep changes focused and match the existing Kotlin / Compose style.
- Run `./gradlew assembleRelease` before opening a PR.
- For a bug report, include your Android version, Stickly version, pack link and steps to reproduce.

---

## <img src="docs/icons/legal-disclaimer.svg" width="24" height="24" alt="" />&nbsp;Legal disclaimer

Stickly is an **unofficial, independent project**. It is not affiliated with, endorsed by, or sponsored by Telegram, WhatsApp or Meta. All trademarks, logos and sticker content belong to their respective owners.
