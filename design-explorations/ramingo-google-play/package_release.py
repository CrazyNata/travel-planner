"""Assemble only the selected EN/RU Google Play listing assets for manual upload."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parent
PACKAGE = ROOT / "release-package"
ICON = ROOT.parents[1] / "design-reference" / "ramingo-google-play-icon-512.png"


def copy(source: Path, relative: str) -> Path:
    target = PACKAGE / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)
    return target


def main() -> None:
    files: list[Path] = []
    files.append(copy(ICON, "shared/play-icon-512.png"))
    for lang in ("en", "ru"):
        source = ROOT / "copy" / lang
        for name in ("app-title.txt", "short-description.txt", "full-description.txt", "image-alt-text.txt"):
            files.append(copy(source / name, f"{lang}/{name}"))
        files.append(copy(ROOT / "feature-graphic" / "final" / ("ru" if lang == "ru" else "") / "feature-graphic.png",
                          f"{lang}/feature-graphic.png"))
        screenshot_source = ROOT / "screenshots" / "final" / ("ru" if lang == "ru" else "")
        for screenshot in sorted(screenshot_source.glob("*.png")):
            files.append(copy(screenshot, f"{lang}/screenshots/{screenshot.name}"))

    readme = """# Ramingo — Google Play upload files

English and Russian store listing assets, prepared for manual entry in Play Console.

- `shared/play-icon-512.png`: the already-approved Google Play icon (512×512 PNG with alpha).
- Each language folder contains the title, short description, full description, screenshot alt text, 1024×500 feature graphic, and seven screenshots in upload order.
- All screenshots are 1240×2208 RGB PNG. The feature graphics are 1024×500 RGB PNG without alpha.

The app icon is included as an unchanged copy. This package contains no adaptive-icon outputs, drafts, or contact sheets. The EN-US and RU feature-graphic replacements were submitted for Google Play review on 19 October 2026; storefront publication is pending review. The icon, listing text and seven screenshots per language were not changed in that submission.
"""
    (PACKAGE / "README.md").write_text(readme, encoding="utf-8")
    files.append(PACKAGE / "README.md")

    manifest = []
    for path in sorted(files):
        data = path.read_bytes()
        entry = {"file": path.relative_to(PACKAGE).as_posix(),
                 "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()}
        if path.suffix.lower() == ".png":
            from PIL import Image
            with Image.open(path) as image:
                entry["dimensions"] = list(image.size)
                entry["mode"] = image.mode
                entry["alpha"] = "A" in image.getbands()
        manifest.append(entry)
    (PACKAGE / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")

    archive = ROOT / "release-package.zip"
    with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for path in sorted(PACKAGE.rglob("*")):
            if path.is_file():
                zf.write(path, path.relative_to(PACKAGE).as_posix())
    print(f"Packaged {len(manifest)} assets plus manifest.json into {archive.name} ({archive.stat().st_size:,} bytes)")


if __name__ == "__main__":
    main()
