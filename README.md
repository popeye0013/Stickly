<p align="center">
  <img src="app/src/main/res/drawable-nodpi/app_icon.png" alt="Stickly app icon" width="96" />
</p>

<h1 align="center">Stickly</h1>

<p align="center">
  Import Telegram sticker packs and export them to WhatsApp.
</p>

<p align="center">
  <a href="https://github.com/popeye0013/Stickly/releases/latest">
    <img src="https://img.shields.io/github/v/release/popeye0013/Stickly?style=flat&label=latest" alt="Latest release" />
  </a>
  <a href="https://github.com/popeye0013/Stickly/releases/download/v1.0/Stickly-v1.0.apk">
    <img src="https://img.shields.io/badge/Download-APK-6750A4?style=flat" alt="Download APK" />
  </a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat" alt="Android 8.0+" />
</p>

## About

Stickly is a native Android app built with Kotlin, Jetpack Compose and Material 3 Expressive. It reads a sticker pack from a `t.me/addstickers/...` link using a Telegram bot token you provide, converts the stickers locally, and sends them to WhatsApp as a proper sticker pack.

The token stays on your device and is only sent to `api.telegram.org`.

## Features

- Imports every sticker in a Telegram pack.
- Converts PNG/WebP, gzipped `TGS` and WebM sources into WhatsApp-ready WebP.
- Makes static packs under 100 KB per sticker and animated packs under 500 KB per sticker.
- Splits large packs into balanced WhatsApp packs of 3–30 stickers.
- Shows an **Added to WhatsApp** badge after a pack is added.
- Lets you tap a sticker to view it and long-press up to 25 stickers to build a new pack.
- Uses Material 3 Expressive with System, Light and Dark themes.
- Provides a **Guide** in Settings for creating a Telegram bot token.
- Uses in-app hints instead of toasts or snackbars.

## How it works

1. Open **Settings → Telegram** and save your bot token. The **Guide** button shows how to create one with [@BotFather](https://t.me/BotFather).
2. On **Home**, paste a pack link such as `t.me/addstickers/DonutTheDog` and tap **Import pack**.
3. Enter a pack name. WhatsApp always shows **Stickly** as the publisher.
4. Open the imported pack and tap **Add to WhatsApp**, then confirm the preview in WhatsApp.

## Download

Download [`Stickly-v1.0.apk`](https://github.com/popeye0013/Stickly/releases/download/v1.0/Stickly-v1.0.apk) from the [v1.0 release](https://github.com/popeye0013/Stickly/releases/tag/v1.0).

```text
SHA-256: 01ce672fbd4130eab369f0d4a1957839a3cd49263729920bf60652d47359659b
```

The APK is also committed in this repository as [`Stickly-v1.0.apk`](Stickly-v1.0.apk).

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
git clone https://github.com/popeye0013/Stickly.git
cd Stickly
./gradlew assembleRelease
```

The output APK is written to:

```text
app/build/outputs/apk/release/app-release.apk
```

## FAQ

**Do I need a bot token?**

Yes. Telegram's Bot API needs a token to read sticker sets. Create one with [@BotFather](https://t.me/BotFather), then save it in Stickly Settings.

**Why is one Telegram pack split into several WhatsApp packs?**

WhatsApp allows only 3–30 stickers per pack. Stickly splits larger sets evenly and names them `Name (1/4)`, `Name (2/4)`, and so on.

**Can static and animated stickers be mixed?**

No. WhatsApp rejects mixed packs. If any sticker animates, Stickly converts the whole pack to animated WebP.

## License

GPL-3.0. See [LICENSE](LICENSE).

Stickly is an unofficial project and is not affiliated with Telegram or WhatsApp.
