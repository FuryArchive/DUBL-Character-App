# GitHub build/release workflow

## Development builds

Normal pushes do not create GitHub Releases.

- `android-ci.yml` builds `Fury-Book-Android-dev.apk` on `main` and pull requests.
- `linux-appimage.yml` runs parity, compile, package and AppImage smoke gates on `main`, pull requests or manual dispatch.
- `windows-desktop.yml` builds and verifies Compose Windows EXE/MSI packages on `main`, pull requests or manual dispatch.

The development Android application ID remains `com.dubl.character.android.dev`. The repository debug keystore is intentionally public and is never used for production releases.

## Unified production release

`.github/workflows/release.yml` is the only workflow that publishes a GitHub Release.

A tag such as `v0.5` publishes **Fury Book 0.5** with:

- `Fury-Book-0.5-Android.apk`
- `Fury-Book-0.5-Linux-x86_64.AppImage`
- `Fury-Book-0.5-Linux-x86_64.AppImage.sha256`
- `Fury-Book-0.5-Windows-x64.exe`
- `Fury-Book-0.5-Windows-x64.msi`

The public version may use two components (`0.5`). Compose native packaging receives a normalized three-component version (`0.5.0`) internally.

Product-level build variables now use `FURY_BOOK_*`. Legacy `DUBL_VERSION*` variables remain accepted by Gradle/package scripts as temporary local compatibility fallbacks; DUBL ruleset identity is unaffected.

Android release `versionCode` stays in the high Fury Book range. `v0.5` maps to `105000000`, allowing updates over the older Android build line.

Release signing still uses these GitHub Actions secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Never delete the only backup of the release keystore: future Android updates must use the same signing identity.

## Releasing 0.5

After the desired commit is on `main`:

```bash
git tag -a v0.5 -m "Fury Book 0.5"
git push origin v0.5
```

The release is published only after Android, Linux and Windows jobs all succeed.
