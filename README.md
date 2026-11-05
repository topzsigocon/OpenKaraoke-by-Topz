# OpenKaraoke 🎤

Offline MIDI/KAR karaoke player para sa Android. Walang internet na kailangan, at **ikaw ang magdadala ng sarili mong mga kanta**.

> ⚠️ Player lang ang OpenKaraoke. Walang kasamang kanta (musika o lyrics) sa repo o sa app. Tingnan ang [Mga paalala](#mga-paalala).

## Mga Feature

- **Library** na kayang humawak ng libo-libong kanta (nasubukan sa 47,000), na may paghahanap ayon sa numero, pamagat, o artist
  - Nababasa ang `.mid`, `.midi`, at `.kar`, pati ang mga nasa loob ng **`.zip`** (hindi na kailangang i-extract)
  - **Paborito** (★) at **Kamakailan** (history)
  - Naka-save ang index, kaya mabilis ang pagbukas ng app
- **Tunog** gamit ang SoundFont (`.sf2`): kasama ang default, at puwedeng pumili ng sarili mo
  - Key (−12 hanggang +12), tempo (50% hanggang 150%), at mute ng bawat channel
  - **I-mute ang melody** nang isang pindot (para ikaw ang kumanta)
  - Naaalala ang key, tempo, at mute **kada kanta**
- **Lyrics** na sumasabay sa tugtog, may sync offset
- **Karaoke Mode**: full-screen na parang tunay na karaoke machine
  - Background video, buong linya ng lyrics na may highlight sa kasalukuyang salita
  - Mga option: Default o Two-line, Center o Bottom, Centered o Opposing
  - Saglit na pahinga sa simula na may pamagat, artist, at numero ng kanta
  - Puwedeng kusang pumasok sa Karaoke Mode sa bawat kanta
- **Floating bubble** (hilahin kahit saan): mag-input ng numero, maghanap, magdagdag sa queue, at kontrolin ang pagtugtog
- **Queue** na naka-save, may susunod-na-kanta at indicator
- **Mic echo/reverb** (Oboe, low latency): gumamit ng headphones
- Audio focus (titigil ang kanta kapag may tumawag)
- **Maraming customization** sa Settings:
  - Lyrics: laki, kulay ng highlight, uri ng titik (ABC / Abc), anino, dilim ng video, countdown na tuldok, orientation
  - Tunog: gain, performance mode (para sa mahihinang phone), i-mute ang melody bilang default
  - Queue: repeat, party mode (random na paborito), shuffle
  - Bubble: laki, opacity, puwesto, kusang pagdilim; keypad na may awtomatikong pagdagdag at vibrate
  - Library: pag-uuri, format ng numero; encoding ng lyrics para sa mga kantang Thai, Hapon, Tsino, atbp.
- **Backup at restore** ng mga setting, paborito, history, at queue

## Paano gamitin

1. I-install ang APK (sideload; hindi ito para sa Play Store, tingnan sa ibaba).
2. Sa unang bukas, bigyan ng **All files access** ang app (kailangan para mabasa ang SD card).
3. Ilagay ang mga file sa isa sa mga folder na ito (sa internal storage o sa SD card):

   ```
   OpenKaraoke/
   ├── songs/        # .mid / .kar / .zip na may MIDI
   ├── soundfonts/   # sarili mong .sf2 (opsyonal)
   └── videos/       # background video (.mp4, .mkv, .webm, .3gp)
   ```

   O pumili ng sarili mong folder sa **Settings → Mga folder**.
4. Sa **Settings**, pindutin ang **I-scan ulit**. Ang unang scan ng libo-libong file ay maaaring tumagal ng ilang minuto; isang beses lang ito.

### Pangalan ng mga kanta

Kinukuha ang numero, pamagat, at artist mula sa pangalan ng file:

| Pangalan ng file | Resulta |
|---|---|
| `00123 - Pamagat - Artist.mid` | numero 123, pamagat, artist |
| `00123_Pamagat_Artist.kar` | numero 123, pamagat, artist |
| `123 Pamagat.mid` | numero 123, pamagat |
| `00123.mid` | numero 123 |
| `Pamagat.mid` | walang numero (hahanapin sa pamagat) |

Kung may kanta sa parehong zip at sa hiwalay na file, hiwalay na file ang gagamitin.

### Karaoke Mode

Pindutin ang **📺** sa player bar o sa lyrics screen. Para bumalik sa normal na UI, pindutin ang **☰ UI** o ang Back. Hindi napuputol ang tunog kapag nagpapalit ng mode. Sa bubble (123), may tab na **Numero**, **Hanapin**, at **Kontrol**.

## Mga paalala

- **Mga kanta:** may copyright ang musika at lyrics ng karamihan sa MIDI/KAR. Huwag mag-upload ng mga kanta sa repo na ito, at siguraduhing may karapatan ka sa mga gamit mo.
- **Hindi para sa Google Play:** gumagamit ito ng *All files access* para sa direktang pagbasa ng SD card. Kung gusto mong i-publish, kakailanganin ng bersyong gumagamit ng Storage Access Framework (nakahanda na ang `StorageProvider` para dito).
- **Mic:** gumamit ng headphones o wired na mic para hindi humiyaw (feedback). Ang Bluetooth ay may dagdag na delay.
- **SoundFont:** ang buong SoundFont ay nasa memorya. Ang mga mas malaki sa mga 100 MB ay maaaring maubusan ng memorya sa mahihinang phone.

## Pag-update ng app

Ang APK ay pinipirmahan ng **nakatakdang key** (`app/openkaraoke-signing.p12`), kaya ang bawat bagong build ay mai-i-install **bilang update sa ibabaw ng lumang app**, nang hindi nabubura ang mga setting at paborito. Kung ang naka-install mong bersyon ay galing sa mas lumang build na iba ang pirma, kailangan mo ng isang beses na pag-uninstall.

Bago mag-uninstall o mag-reinstall, gamitin ang **Settings → Backup at restore**. Nase-save ang backup sa `OpenKaraoke/openkaraoke-backup.zip` sa internal storage.

> Ang key na ito ay para sa **personal na paggamit lamang** at nasa repo (hindi sikreto). Kung ipa-publish mo ang app, gumawa ng hiwalay at pribadong key at ilagay ito sa GitHub Secrets.

## Paano i-build

**Kailangan:** JDK 17, Android SDK (API 35), NDK at CMake 3.22.1 (kusang ini-install ng Android Studio / Gradle), at **internet sa unang build**.

```
gradle assembleDebug          # APK sa app/build/outputs/apk/debug/
gradle testDebugUnitTest      # mga unit test
```

Sa unang build, kusang dina-download ng Gradle ang mga ito (hindi sila nasa repo):

- `tsf.h`, ang [TinySoundFont](https://github.com/schellingb/TinySoundFont) (MIT)
- `default.sf2`, ang [GeneralUser GS](https://github.com/mrbumpy409/GeneralUser-GS), ang default na SoundFont. Kung mag-fail ang download, magbi-build pa rin; kailangan mo lang ng sarili mong `.sf2`.

### GitHub Actions

| Workflow | Kailan tumatakbo | Para saan |
|---|---|---|
| **Android Build** (`build.yml`) | sa bawat push sa `main` (maliban kung zip lang) | nagbi-build ng APK at nagpapatakbo ng mga test |
| **Unpack zip and build** (`unpack.yml`) | kapag may `.zip` sa root ng repo, o manu-mano | bubuksan ang zip, ilalagay ang laman sa repo, at magbi-build |

Ang pangalawa ay para sa pag-update **mula sa phone lang**: i-upload ang `update.zip` sa pinakaunang antas ng repo (Add file → Upload files), at ang workflow na ang bahala. Para magbura ng mga lumang file, maglagay ng `REMOVE.txt` sa zip (isang path kada linya). Hindi nababago ng workflow ang `.github/`.

Ang APK ay nasa **Artifacts** ng run (zip ito; i-extract ang `.apk`).

## Istruktura

```
app/src/main/
├── cpp/                  # synth.c (TinySoundFont JNI), mic.cpp (Oboe), CMakeLists.txt
├── java/com/topz/karaoke/
│   ├── audio/            # AudioEngine, MidiSequencer, Player, mic
│   ├── data/             # Song, parser ng pangalan, index cache, settings, zip scanner
│   ├── midi/             # MIDI/KAR parser, TempoMap, lyrics extractor
│   ├── storage/          # pagbasa ng mga file (internal at SD card)
│   └── ui/               # Compose: Library, Lyrics, Karaoke Mode, Settings, bubble
└── res/                  # icon at tema
```

## Roadmap

- [ ] Scoring (kailangan ng pagsubok sa mic)
- [ ] TV/HDMI output
- [x] Mas maraming customization sa Settings
- [x] Backup at restore, at nakatakdang signing key
- [ ] Bersyong Play Store-friendly (Storage Access Framework)

## Mga lisensya

Tingnan ang [LICENSE](LICENSE) para sa code, at ang [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) para sa TinySoundFont, GeneralUser GS, at Oboe. Hindi sakop ng lisensya ng code ang mga kanta at SoundFont na gagamitin mo.
