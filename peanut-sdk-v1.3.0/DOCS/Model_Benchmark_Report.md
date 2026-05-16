# Model Benchmark Report
**Project:** Keenon Peanut T10 — Supermarket Tray Detection
**Device:** RK3288 | Android 6.0.1 | armeabi-v7a (CPU only, no NPU)
**Date:** May 2026

---

## What we tested and why

We placed common supermarket items (water bottles, snacks, chocolates) on the robot tray and ran each model live on the robot. Goal was to find a model that detects the right items, gives the right count, and runs fast enough to be usable.

---

## Results

| Model | Speed | Classes | What went wrong |
|---|---|---|---|
| SSD MobileNetV3 | ~700 ms | 80 | Very noisy — detects background, wrong labels |
| EfficientDet-Lite0 | ~900 ms | 80 | Misses most supermarket items, count unstable |
| EfficientDet-Lite1 | ~1200 ms | 80 | Slightly better accuracy, same class problem |
| EfficientDet-Lite2 | ~1600 ms | 80 | Too slow, still only 80 COCO classes |
| Open Images V7 | ~1300 ms | 601 | Better class coverage, but issues remain (see below) |

---

## Open Images V7 — current best but not sufficient

This was the best model we tried. It covers supermarket-relevant classes (Bottle, Chocolate, Biscuit, Candy, Snack, Pen, Notebook) that COCO completely misses.

**Problems observed on robot:**
- Speed is ~0.5 fps (1300 ms per inference) — too slow for smooth counting
- Item count is inconsistent — same items detected as 1 on one frame, 2 on the next
- Background objects (person walking past, table edge, hands) get detected and added to tray count
- Confidence scores are low and noisy — threshold tuning helps partially but doesn't fully solve it
- Some items still not detected or misclassified (e.g. bottles called "car" when label map fails to load)

---

## Root cause

All models above are general-purpose, trained on internet images. They have never seen a supermarket tray from above under robot lighting. The detection zone sliders help reduce background noise but the model itself cannot reliably distinguish "item on tray" from "hand reaching for item" or "floor visible at edge."

---

## Recommendation — Custom Trained Model

None of the pre-trained models are reliable enough for production use. The right solution is a small custom YOLOv5n or YOLOv8n model trained on photos taken from the actual robot camera.

**What this means practically:**
- Take 200–500 photos per product using the robot's tray cameras
- Label the products (can be done in Roboflow, free tier is enough)
- Train YOLOv5n — takes a few hours on Google Colab (free GPU)
- Export to TFLite and drop into the app — no code changes needed

**Expected outcome:** 5–10 fps, correct item count, no background false positives, works for exactly the products placed on the tray.

This is the standard approach for retail/kiosk robots and is the only way to get production-grade accuracy for specific SKUs.