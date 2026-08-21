<div align="center">

<img width="1280" height="640" alt="Frame 25" src="https://github.com/user-attachments/assets/d801de28-eac6-4ffd-8474-55d9a8af4dc3" />


# Expressive Cutout

**An offline dynamic island that follows Google's Material Expressive design.**

Expressive Cutout turns the camera cutout on your Android phone into a small, living
island. Notifications land in it, system events flash through it, and live tiles keep
what's playing, who's calling and how long is left on your timer within a glance of the
top of your screen — all styled with Material Expressive shapes, springs and Material You
colours, and all customisable down to the corner radius.

It runs entirely on your device. The app has no internet permission at all, so nothing can
be uploaded: no accounts, no analytics, no tracking.

[![Download the latest release](https://img.shields.io/badge/Download-latest%20release-005AC1?style=for-the-badge&logo=github&logoColor=white)](https://github.com/EvanKoe/expressive-cutout/releases)
[![Stars](https://img.shields.io/github/stars/EvanKoe/expressive-cutout?style=for-the-badge)](https://github.com/EvanKoe/expressive-cutout/stargazers)
[![License](https://img.shields.io/badge/license-GPLv3-blue?style=for-the-badge)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/SNfcuuJeYF)

</div>

---

## Table of contents

- [Screenshots](#screenshots)
- [Download](#download)
- [Features](#features)
- [Special thanks](#special-thanks)
- [License](#license)

---

## Screenshots

<!-- Screenshots go here. -->
| | | |
|-|-|-|
|<img width="864" height="1939" alt="Screenshot_20260729-153011" src="https://github.com/user-attachments/assets/9f7e4724-bd91-4486-8f0e-9500906024d2" />|<img width="1080" height="2424" alt="Screenshot_20260729-153006" src="https://github.com/user-attachments/assets/e803d2a2-8e9b-41e0-8688-54affaefa355" />|<img width="864" height="1939" alt="Screenshot_20260729-153043" src="https://github.com/user-attachments/assets/84f40ce3-7ef0-41b4-9d89-3790008de5bc" />|
| <img width="1080" height="2424" alt="Screenshot_20260807-111655" src="https://github.com/user-attachments/assets/7c4b132c-54ec-471b-a4c3-bf7ddc7d8846" /> | <img width="1080" height="2424" alt="Screenshot_20260807-111746" src="https://github.com/user-attachments/assets/1d59ac95-0dfb-4b4b-9b8c-2814eb436da1" /> | <img width="1080" height="2424" alt="Screenshot_20260807-112035" src="https://github.com/user-attachments/assets/33542dec-9d6b-456a-bc14-c779c1d07a00" /> |
 | <img width="1080" height="2424" alt="Screenshot_20260807-112215" src="https://github.com/user-attachments/assets/e33224c9-fe29-4fa2-b580-e0e8a10c7237" /> |

---

## Download

<div align="center">

[![Download the latest release](https://img.shields.io/badge/Download-latest%20release-005AC1?style=for-the-badge&logo=github&logoColor=white)](https://github.com/EvanKoe/expressive-cutout/releases)

</div>

Requires Android 10 (API 29) or newer. On first launch the app walks you through the three
things it needs: notification access (to mirror notifications), the accessibility service
(to draw the overlay), and optionally an exemption from battery
optimisation so the island stays reliable in the background.

Join our [Discord server](https://discord.gg/SNfcuuJeYF) for feedback, feature requests, bug report and share with the Expressive by Evan community!

---

## Features

DISCLAIMER: This app was made as a project to learn Jetpack Compose. I used AI to help me.

| Category | Features |
| --- | --- |
| **Notifications** | Mirrors any app's notifications in the island · icons taken from the notification itself · auto-expand to show title and text, or keep it to a bare app icon · action buttons and inline reply, right from the island · swipe to dismiss clears the notification from the system too |
| **Music tile** | Now-playing title and artist · album art on the collapsed island, optionally spinning while playback runs and stopping when paused · previous / play-pause / next controls on the expanded island |
| **Phone tile** | Live ongoing call with the caller's name, contact photo and running duration · the dialer's own call actions, such as Hang up · incoming-call view with the caller's number and answer / decline buttons, opening the in-call screen on tap · optional taller two-row incoming layout with full-width buttons |
| **Timer tile** | Mirrors the running countdown from your clock app, including Android 16+ live-update timers · the clock app's real buttons and labels, such as Add 1 min and Reset · Pause flips to Resume live |
| **System events** | Charging started and stopped · battery low · Wi-Fi connected and lost · headphones in and out · USB connected and removed · device unlocked |
| **Per-event customisation** | Pick any icon from the full Material icon library, with a search filter · animated icons with an optional loop · per-event colour override · per-event on-screen duration · one Material You colour for every event at once |
| **Size & position** | Independent width and height for the normal and expanded island · corner radius set for all corners, top / bottom, or each corner on its own · vertical and horizontal position · live preview with a light / dark background toggle · reset to default |
| **Appearance** | Solid or gradient background, separately for the normal and expanded island, with direction and opacity · soft shadow · outline stroke with its own width and colour · Material You, preset or custom hex colours everywhere · light, dark or system app theme |
| **Action buttons** | Four button styles (Expressive tonal, Expressive filled, Material You, Outlined) · four reply-field styles, including a segmented bar · button colour and height · cancel button on the left or beside send · tile-specific button colours for the phone and timer tiles |
| **Animations** | Expressive spring or ease-in-out style · slow / default / fast speed · big / normal / small bounce · animation duration scaled from 0 to 1000 ms · tuned against a live example |
| **Behaviour & gestures** | Auto-collapse after a delay, or stay expanded until tapped · disappear entirely when it shrinks · swipe up to shrink · swipe sideways to dismiss, with a direction and a choice of which sizes it applies to · hide on lockscreen, tearing the overlay down completely so it uses no resources |
| **Privacy** | No internet permission at all, so nothing can leave the device · no analytics or tracking · screen content is read for features only, never stored and never sent · every permission explained in-app, and the whole manifest is documented on the Permission details screen |
| **Testing** | Send yourself a test notification with reply and action buttons · trigger a test ongoing call and a test incoming call · preview any system event on the island from its detail screen |

---

## Permissions

Your data privacy matters. The app only requests the absolute minimum permissions required to function. Plus, it has no INTERNET access permission, which means nothing is sent on the Internet: no analytics, no usage data, no notification content stored anywhere. Everything stays on your phone. Always. Here are the required permissions:
- Notification access: to display your notifications in the dynamic island,
- Accessibility service: to display over other apps. It may also read screen content, but only to make features work: it reads which app is in the foreground (so a tile can hide while you are in that app), and it reads answer text from assistant apps so the assistant tile can show you the answer. That is all it looks at — every other app is a package name and nothing more. Whatever is read is drawn on the island and immediately dropped: it is never written to storage, never logged, and — since the app has no internet permission — cannot be sent anywhere,  
- Ignore battery optimisation (optional): to make sure battery optimisation does not kill the app,  
- Post notification (optional): for testing purpose,  
- Network state: it can just read if you are connected to the internet (for the Wifi event), but cannot access it

## Special thanks

<!-- Special thanks go here. -->
Even if this project started as a personal app, a lot of existing projects gave me the inspiration I needed, may it be for UI or features: 
- [Dynamic spot](https://play.google.com/store/apps/details?id=com.jamworks.dynamicspot&hl=en)  
- [Material Capsule](https://play.google.com/store/apps/details?id=com.pryshedko.mtisland&hl=en)

And my very special thanks to [Sameerasw's Essential app for Android](https://github.com/sameerasw/essentials) which has an AWESOME Material You Expressive implementation. He is doing a great job and all his apps feel very polished. 

I would like to thank everyone that takes part to the project on Discord, Github and Reddit by submitting bugs, requesting features and creating issues.

---

## License

Expressive Cutout is free software, licensed under the
[GNU General Public License v3.0](LICENSE). You may use, study, share and modify it; any
distributed derivative must remain under the same licence.
