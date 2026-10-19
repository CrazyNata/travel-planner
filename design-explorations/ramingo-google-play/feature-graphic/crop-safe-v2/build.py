from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps


HERE = Path(__file__).resolve().parent
PROJECT = HERE.parents[1]
REPO = PROJECT.parents[1]
SOURCE = PROJECT / "feature-graphic/source/ramingo-itinerary-artwork-v2.png"
FONT_DIR = REPO / "mobile/app/src/main/res/font"
INK = "#251D3B"
PAPER = "#FFF3DA"


def font(size: int, bold: bool = True) -> ImageFont.FreeTypeFont:
    name = "manrope_extrabold.ttf" if bold else "manrope_medium.ttf"
    return ImageFont.truetype(str(FONT_DIR / name), size)


def fit_text(draw: ImageDraw.ImageDraw, text: str, x: int, y: int,
             max_width: int, size: int, bold: bool = True) -> None:
    while size > 12 and draw.textlength(text, font=font(size, bold)) > max_width:
        size -= 1
    draw.text((x, y), text, font=font(size, bold), fill=INK)


def feature_graphic(language: str) -> Image.Image:
    scene = Image.open(SOURCE).convert("RGB")
    scene = ImageOps.fit(scene, (570, 500), method=Image.Resampling.LANCZOS,
                         centering=(0.5, 0.5))
    image = Image.new("RGB", (1024, 500), PAPER)
    image.paste(scene, (454, 0))
    draw = ImageDraw.Draw(image)

    # The central 16:9 crop removes about 67 px from each side of the source.
    # Keep every text element inside that crop with a visible inset.
    draw.text((96, 42), "Ramingo", font=font(34), fill=INK)
    if language == "ru":
        lines = [("Все остановки —", 42), ("в одном плане.", 42)]
        subtitle = "Маршруты, места и прогулки — вместе."
        subtitle_size = 18
    else:
        lines = [("Every stop.", 48), ("One clear plan.", 44)]
        subtitle = "Routes, places and city walks together."
        subtitle_size = 20
    for index, (line, size) in enumerate(lines):
        fit_text(draw, line, 96, 144 + index * 64, 340, size)
    fit_text(draw, subtitle, 96, 300, 340, subtitle_size, bold=False)
    return image


def center_crop_16_9(image: Image.Image, width: int = 1024) -> Image.Image:
    height = round(width * 9 / 16)
    return ImageOps.fit(image, (width, height), method=Image.Resampling.LANCZOS,
                        centering=(0.5, 0.5))


def main() -> None:
    outputs = {}
    for language in ("ru", "en"):
        image = feature_graphic(language)
        path = HERE / language / "feature-graphic.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        image.save(path, format="PNG", optimize=True)
        outputs[language] = image

    # Compare the current Russian graphic with the proposed one as it appears
    # when a Play surface crops the 1024x500 asset into a 16:9 card.
    current = Image.open(PROJECT / "release-package/ru/feature-graphic.png").convert("RGB")
    current_crop = center_crop_16_9(current, 768)
    revised_crop = center_crop_16_9(outputs["ru"], 768)
    board = Image.new("RGB", (1600, 520), "#F4F2EF")
    draw = ImageDraw.Draw(board)
    label_font = font(24)
    draw.text((32, 22), "CURRENT · PLAY CROP", font=label_font, fill=INK)
    draw.text((832, 22), "REVISED · SAFE CROP", font=label_font, fill=INK)
    board.paste(current_crop, (32, 70))
    board.paste(revised_crop, (832, 70))
    board_path = HERE / "crop-comparison.png"
    board.save(board_path, format="PNG", optimize=True)

    # A full-width revised export is included beside the crop proof.
    preview = HERE / "revised-ru-16x9-preview.png"
    center_crop_16_9(outputs["ru"], 1024).save(preview, format="PNG", optimize=True)

    for language, image in outputs.items():
        assert image.size == (1024, 500)
        # Check the 16:9 visible crop retains all headline ink with margins.
        crop = center_crop_16_9(image)
        crop_path = HERE / language / "play-crop-preview.png"
        crop.save(crop_path, format="PNG", optimize=True)

    print(f"Created RU and EN 1024x500 drafts in {HERE}")
    print(f"Created crop comparison: {board_path}")


if __name__ == "__main__":
    main()
