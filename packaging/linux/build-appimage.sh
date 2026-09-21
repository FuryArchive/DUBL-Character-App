#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VERSION="${FURY_BOOK_VERSION:-${DUBL_VERSION:-0.5.0}}"
ARCH="x86_64"
DIST="$ROOT/dist"
APPDIR="$ROOT/build/appimage/FuryBook.AppDir"
APPIMAGE="$DIST/Fury-Book-${VERSION}-linux-${ARCH}.AppImage"
APPIMAGETOOL_VERSION="1.9.1"
APPIMAGETOOL_SHA256="ed4ce84f0d9caff66f50bcca6ff6f35aae54ce8135408b3fa33abfc3cb384eb0"
APPIMAGETOOL_URL="https://github.com/AppImage/appimagetool/releases/download/${APPIMAGETOOL_VERSION}/appimagetool-x86_64.AppImage"
APPIMAGETOOL="${APPIMAGETOOL:-$ROOT/build/appimage/appimagetool-x86_64.AppImage}"

mkdir -p "$DIST" "$(dirname "$APPIMAGETOOL")"

# Canonical product build: Compose Desktop + bundled JVM.
"$ROOT/gradlew" :desktopApp:createDistributable

SOURCE="$ROOT/desktopApp/build/compose/binaries/main/app/FuryBook"
[[ -d "$SOURCE" ]] || { echo "Compose distributable not found: $SOURCE" >&2; exit 1; }
rm -rf "$APPDIR"
mkdir -p "$APPDIR/usr/lib/fury-book" "$APPDIR/usr/bin" "$APPDIR/usr/share/applications"
cp -a "$SOURCE/." "$APPDIR/usr/lib/fury-book/"
ln -s ../lib/fury-book/bin/FuryBook "$APPDIR/usr/bin/fury-book"
cat > "$APPDIR/AppRun" <<'APPRUN'
#!/usr/bin/env sh
HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
exec "$HERE/usr/bin/fury-book" "$@"
APPRUN
chmod +x "$APPDIR/AppRun"
cat > "$APPDIR/fury-book.desktop" <<'DESKTOP'
[Desktop Entry]
Type=Application
Name=Fury Book
Comment=Fury Book tabletop RPG character and rules platform
Exec=fury-book
Icon=fury-book
Terminal=false
Categories=Game;Utility;
DESKTOP
cp "$APPDIR/fury-book.desktop" "$APPDIR/usr/share/applications/fury-book.desktop"
cat > "$APPDIR/fury-book.svg" <<'SVG'
<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256" viewBox="0 0 256 256">
  <rect width="256" height="256" rx="52" fill="#111318"/>
  <rect x="18" y="18" width="220" height="220" rx="38" fill="none" stroke="#2B2F38" stroke-width="4"/>
  <path d="M73 58h118v30h-82v38h66v29h-66v69H73V58z" fill="#F4F0E8"/>
  <path d="M174 157l33 0v67h-33z" fill="#B53C4C"/>
</svg>
SVG
ln -s fury-book.svg "$APPDIR/.DirIcon"

if [[ ! -f "$APPIMAGETOOL" ]]; then
  curl -fsSL "$APPIMAGETOOL_URL" -o "$APPIMAGETOOL"
fi
printf '%s  %s\n' "$APPIMAGETOOL_SHA256" "$APPIMAGETOOL" | sha256sum -c -
chmod +x "$APPIMAGETOOL"

ARCH="$ARCH" APPIMAGE_EXTRACT_AND_RUN=1 "$APPIMAGETOOL" "$APPDIR" "$APPIMAGE"
chmod +x "$APPIMAGE"
sha256sum "$APPIMAGE" > "$APPIMAGE.sha256"
echo "Built: $APPIMAGE"
