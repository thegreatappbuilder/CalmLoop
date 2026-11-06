# Calm Loop — sunete relaxante pentru Android

Aplicație gratuită cu sunete în buclă infinită (ploaie, ploaie tropicală, tunete, valuri, vânt, pârâu, foc de tabără, zgomot alb/roz/maro, muzică de meditație, boluri tibetane, unde theta), muzică proprie, temporizator de somn, popup-uri și reclame AdMob controlate de la distanță.

- Funcționează pe Android 7.0 și mai nou (~99% din telefoane).
- Sunetele sunt generate în timp real: nu se aude nicio „tăietură" la reluare și aplicația are doar câțiva MB.
- Se pot combina mai multe sunete, fiecare cu volumul lui, plus o melodie de pe telefon.
- Merge și cu ecranul stins (notificare cu Pauză / Oprește).

---

## 1. Obține APK-ul (fără să instalezi nimic pe calculator)

1. Fă-ți cont gratuit pe **github.com**.
2. Apasă **+ → New repository**, nume `CalmLoop`, alege **Private**, apasă **Create repository**.
3. Pe pagina repository-ului: **uploading an existing file** → trage în pagină **tot conținutul** folderului `CalmLoop` (inclusiv folderele `.github`, `app`, `gradle`) → **Commit changes**.
   _De pe calculator e cel mai simplu; GitHub nu poate dezarhiva un .zip, așa că dezarhivează întâi._
   _Dacă folderul `.github` nu apare (e ascuns pe unele calculatoare): **Add file → Create new file**, scrie numele `.github/workflows/build.yml` și lipește conținutul fișierului cu același nume din arhivă._
4. Mergi la **Settings → Secrets and variables → Actions → New repository secret** și adaugă cele 4 secrete din fișierul `CHEIE-SEMNARE-PASTREAZ-O.txt` (`KEY_ALIAS`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD`, `KEYSTORE_BASE64`).
5. Tabul **Actions** → **Build Calm Loop** → **Run workflow**. După ~5–8 minute, jos la **Artifacts** găsești:
   - **CalmLoop-APK** → fișierul `.apk` pe care îl instalezi direct pe telefon;
   - **CalmLoop-PlayStore-AAB** → fișierul `.aab` pe care îl încarci în Play Store.

Alternativ: deschide folderul în **Android Studio** (gratuit) → **Build → Build APK(s)**.

## 2. Setările tale (fișierul `app/build.gradle.kts`, sus de tot)

| Ce | Unde |
|---|---|
| ID-urile AdMob (app, banner, interstitial) | `admobAppId`, `admobBannerId`, `admobInterstitialId` — acum sunt ID-uri de TEST Google |
| Linkul fișierului de control | `remoteConfigUrl` = `https://raw.githubusercontent.com/UTILIZATOR/CalmLoop/main/config/app-config.json`* |
| Politica de confidențialitate | `privacyPolicyUrl` |
| Versiunea (la fiecare actualizare în Play Store) | `versionCode` +1 și `versionName` |

\* Linkul „raw" funcționează doar dacă repository-ul e **public**. Dacă îl ții privat, pune fișierul `app-config.json` într-un **Gist public** (gist.github.com) și folosește linkul „Raw" al lui.

## 3. Popup-uri și reclame „când vrei tu" — fără versiune nouă

Editezi `config/app-config.json` (chiar de pe telefon, pe GitHub) și salvezi. Aplicația citește fișierul la fiecare pornire.

**Popup:**
- `"enabled": true` îl pornește, `false` îl oprește.
- Schimbă `"id"` (ex. `"oferta-craciun"`) ca să apară un popup **nou** — fiecare id apare o singură dată pe telefon dacă `"show_once": true`; cu `false` apare la fiecare pornire.
- `title`, `message`, `button_text` = textele; `link_url` + `link_text` = buton opțional care deschide un link (ex. pagina ta din Play Store).
- `delay_seconds` = după câte secunde apare.

**Reclame:**
- `"enabled": false` → nicio reclamă.
- `"banner"` → bannerul de jos, pornit/oprit.
- `"interstitial"` → reclama pe tot ecranul, pornită/oprită.
- `"interstitial_every_n_plays": 5` → apare la fiecare a 5-a pornire de sunet.
- `"interstitial_min_interval_seconds": 180` → cel puțin 3 minute între ele.

## 4. AdMob (ca să câștigi bani)

1. Cont pe **admob.google.com** → **Apps → Add app** (Android, „nu e încă publicată").
2. Creează 2 unități: **Banner** și **Interstitial**. Copiază ID-urile în `app/build.gradle.kts`.
3. **Privacy & messaging → GDPR** → creează și publică mesajul de consimțământ (obligatoriu în UE; aplicația îl afișează automat).
4. După publicarea în Play Store, leagă aplicația din AdMob de cea din magazin.

⚠️ Nu da click pe reclamele tale reale — AdMob îți poate închide contul. Pentru teste folosește ID-urile de test care sunt deja puse.

## 5. Publicare în Google Play

1. Cont pe **play.google.com/console** (taxă unică 25 $, verificare identitate).
2. **Create app** → nume „Calm Loop" (sau altul), Gratuită.
3. Completează: politica de confidențialitate (vezi `privacy-policy.md` — completează-ți numele/emailul și publică-l, ex. pe Google Sites sau un Gist), „Ads: Da", Data safety (AdMob colectează ID de publicitate), rating de conținut, public țintă 13+.
4. **Foreground service**: declari „Media playback — redare sunete relaxante în fundal".
5. Grafică: `store-assets/icon-512.png`, `store-assets/feature-graphic-1024x500.png` + 2–8 capturi de ecran de pe telefon.
6. **Conturile personale noi** trebuie întâi să facă un **test închis cu minimum 12 testeri, timp de 14 zile**, apoi pot cere acces la producție.
7. Încarci fișierul `.aab` și trimiți la verificare.

## 6. Schimbarea numelui pachetului (opțional, înainte de prima publicare)

`applicationId = "com.calmloop.sounds"` din `app/build.gradle.kts` trebuie să fie unic în Play Store. Dacă e ocupat, schimbă-l (ex. `ro.numeletau.calmloop`). După publicare nu se mai poate schimba.
