# TFLite Detection Models

The YOLO tab TFLite pipeline loads models from this directory.
The spinner in the YOLO tab lists all available models — select the one you want.

---

## Models already included

| File | Classes | Notes |
|---|---|---|
| `efficientdet_lite0.tflite` | 80 COCO | Default — fast, good balance |
| `efficientdet_lite1.tflite` | 80 COCO | Better accuracy, slightly slower |
| `efficientdet_lite2.tflite` | 80 COCO | Best accuracy of the Lite series |
| `ssd_mobilenet_v3.tflite`   | 80 COCO | Fastest, lowest accuracy |

---

## Open Images V7 — 600 classes (DOWNLOAD REQUIRED)

The spinner shows **"Open Images V7 (600 classes)"** but the model file is NOT
included in the APK (it is ~15 MB). Download it once and place it here.

### What to download

Go to TensorFlow Hub:
```
https://tfhub.dev/tensorflow/lite-model/efficientdet/lite2/detection/metadata/1
```
Click **"Download"** — you get a `.tflite` file.

### What to rename it

Rename the downloaded file to exactly:
```
open_images_v7.tflite
```

### Where to put it

```
SampleApp/app/src/main/assets/models/open_images_v7.tflite
```

Then rebuild the APK — the spinner entry will now work.

### Why Open Images V7?

Open Images V7 has 600 object classes vs COCO's 80. For supermarket use,
it covers classes COCO does NOT have:
- `Snack`, `Candy`, `Biscuit`, `Chocolate`, `Confectionery`
- `Bottle` (more types), `Drink`, `Food`
- `Pencil`, `Pen`, `Notebook`, `Stationery`
- `Packaged goods` and many more

This makes it much better for detecting random supermarket items on the tray.

---

## Custom SKU training (future)

Train your own model on actual photos of the products you put on the tray.
Export as `.tflite`, name it anything, place it here, and add it to
`MODEL_PATHS` / `MODEL_NAMES` in `YoloFragment.java`.
No other code changes needed — `ObjectDetectorHelper` reads tensor shapes
from the model at runtime.
