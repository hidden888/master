# StreamDeck TV

Ein **eigener IPTV-Player für Android TV**, der sich mit einem **Xtream-Codes**-Anbieter
verbindet. Eigenständige App, eigener Code – inspiriert von der TiViMate-Bedienlogik
(Sidebar, Senderliste nach Kategorien, Live-Wiedergabe per Fernbedienung), aber ohne
fremde/geschützte Codebestandteile.

> Hinweis: Dieses Projekt enthält **keinerlei** kopierten Code oder Assets aus TiViMate.
> Es ist eine von Grund auf neu entwickelte App.

## Funktionsumfang (Stand: MVP / Phase 1)

- **Konto-Login** gegen die Xtream-Codes-`player_api.php` (Server-URL, Benutzer, Passwort),
  Passwort verschlüsselt im Android Keystore gespeichert.
- **Live-TV**: Kategorien-Liste + Senderliste (mit Logos), vollständig D-Pad-bedienbar.
- **Wiedergabe** via Media3/ExoPlayer mit HLS- und MPEG-TS-Unterstützung
  (automatischer `.m3u8` → `.ts` Fallback bei Fehlern).
- Sidebar-Navigation (Live / Filme / Serien / Favoriten / Programm / Einstellungen) –
  weitere Bereiche sind als Platzhalter angelegt (siehe Roadmap).

## Tech-Stack

- Kotlin, **Jetpack Compose for TV** (`androidx.tv:tv-material`, `tv-foundation`)
- **Media3 / ExoPlayer** (`media3-exoplayer`, `-hls`, `-datasource-okhttp`)
- **Retrofit + OkHttp + kotlinx.serialization** (lenient – Xtream liefert Zahlen oft als Strings)
- **Room** (Offline-Cache: Konten, Kategorien, Sender)
- **Hilt** (DI), **Coil** (Bilder), **DataStore** (Session), **security-crypto** (Keystore)
- Architektur: MVVM + Repository, Schichten `domain` / `data` / `ui`

## Projektstruktur

```
app/src/main/java/com/iptv/player/
  core/        network (NetworkResult), util (UrlBuilder, SessionManager, CredentialCrypto),
               ui/theme, ui/components (TvTextField, ErrorView, LoadingIndicator)
  data/        remote (XtreamApiService, DTOs, lenient Serializer),
               local (AppDatabase, Entities, DAOs), mapper, repository
  domain/      model, repository (Interfaces)
  di/          NetworkModule, DatabaseModule, RepositoryModule, PlayerModule
  ui/          onboarding (Login), main (Sidebar), live, player, navigation
```

## Build

Voraussetzungen:

- JDK 17+
- **Android SDK** (`ANDROID_HOME` / `local.properties` mit `sdk.dir=...`), Platform 34
- Netzwerkzugriff auf **Google Maven** (`dl.google.com` / `maven.google.com`) für AGP & AndroidX

```bash
./gradlew assembleDebug          # Debug-APK bauen
./gradlew test                   # JVM-Unit-Tests (UrlBuilder, DTO-Parsing)
./gradlew installDebug           # auf verbundenes Gerät/Emulator installieren
```

### Android-TV-Emulator

```bash
sdkmanager "system-images;android-34;android-tv;x86_64"
avdmanager create avd -n tv34 -k "system-images;android-34;android-tv;x86_64" -d tv_1080p
emulator -avd tv34
./gradlew installDebug
```

Bedienung im Emulator: Pfeiltasten = D-Pad, Enter = OK/CENTER, Esc = BACK.

## Verifikation

1. **Unit-Tests**: `./gradlew test` – prüft URL-Bau und das tolerante JSON-Parsing
   (Zahlen als Zahl *oder* String).
2. **Manuell** mit einem Test-Xtream-Account: App starten → Server-URL/Benutzer/Passwort
   eingeben → „Verbinden" → Kategorien & Sender erscheinen → Sender auswählen → Live-Bild.

## Roadmap (geplante Phasen)

- **Phase 2 – EPG**: Now/Next via `get_short_epg`, XMLTV-Guide-Grid, Channel-Surf-Overlay.
- **Phase 3 – VOD & Serien**: Katalog, Detailseiten, Folgen, Wiedergabe.
- **Phase 4 – Favoriten & Multi-Profile**: mehrere Konten, Profile, Eltern-PIN.
- **Phase 5 – Politur**: Aufnahme/Catch-up, EPG-Auto-Refresh, Einstellungen, Resume.

## Bekannte Einschränkung der Build-Umgebung

In der hier verwendeten Cloud-Umgebung ist `dl.google.com` (Google Maven) durch die
Netzwerk-Policy blockiert (HTTP 403), und es ist kein Android SDK installiert. Daher
konnte der Gradle-Build hier **nicht** ausgeführt werden. Der Code ist vollständig und
baufertig; ein `assembleDebug` gelingt in einer Umgebung mit Android SDK und Zugriff auf
Google Maven. Siehe https://code.claude.com/docs/en/claude-code-on-the-web zur
Konfiguration der Netzwerk-Policy.

## Rechtliches

StreamDeck TV ist ein reiner Player. Es werden keine Inhalte bereitgestellt; ein gültiger
Xtream-Codes-Zugang eines Anbieters ist erforderlich. Nutze nur Inhalte, zu denen du
berechtigt bist.
