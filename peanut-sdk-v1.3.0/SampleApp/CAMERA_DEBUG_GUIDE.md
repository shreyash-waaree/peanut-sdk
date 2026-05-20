# Camera problems on the robot — simple debug guide

Use this when **Camera**, **YOLO**, or **COUNT** tabs fail to open preview or crash when switching **0 → 1 → 2**.

---

## What we already know about your robot (from ADB)

| Check | Your robot |
|--------|------------|
| ADB | Device `GFEFQIBXIL` |
| Camera HAL | RockChip `RK29_ICS_CameraHal_Module` |
| Android camera indices | **4** (0, 1, 2, 3) |
| Tray cameras in SampleApp | Use **0, 1, 2** only |
| Video nodes | `/dev/video7` … `video10` |
| App installed | `com.keenon.peanut.sample` (check date with steps below) |

Right now, if **Active Camera Clients** is empty, no app is holding the camera — good for testing.

---

## The 3 usual causes (simple words)

1. **Empty parameter** — App asks the driver for settings; driver returns nothing → “get parameters failed”.
2. **HAL crashed** — You switch camera too fast; Android camera service dies → “HAL crashed”.
3. **Another app has the camera** — Food delivery / Keenon service uses `mx_camera` → SampleApp cannot open.

---

## Step-by-step: capture the exact error

### Step 1 — Confirm PC sees the robot

```powershell
adb devices
```

You must see one line ending with **`device`** (not `offline` or `unauthorized`).

### Step 2 — See if something else is using the camera

```powershell
adb shell dumpsys media.camera | findstr /i "Active Camera Clients Number"
```

- **`Active Camera Clients: []`** → nobody is using it; OK to test.
- If you see a **package name** (e.g. `com.keenon.peanut.solutionfooddelivery`) → that app has the camera. Close it on the robot, then test again.

### Step 3 — Clear logs and reproduce on the robot

**On PC:**

```powershell
adb logcat -c
```

**On robot (do this within 30 seconds after clearing):**

1. Open **Sample App** → **Developer**
2. **Camera** tab → tap **Start** on camera **0**
3. Tap button **1** (switch camera)
4. Note the **exact toast** text

Repeat on **COUNT** or **YOLO** if those fail too.

### Step 4 — Save logs to a file

```powershell
cd "d:\OneDrive - Waaree Energies Limited\Peanut_service apk file\peanut-sdk-v1.3.0\peanut-sdk-v1.3.0\SampleApp\scripts"
.\capture-camera-logs.ps1
```

Or one manual command after you reproduce:

```powershell
adb logcat -d -v time > %USERPROFILE%\Desktop\robot-camera-log.txt
```

### Step 5 — Read the log (what to search for)

| If you see this in logcat | Meaning |
|---------------------------|---------|
| `get parameters failed` / `empty parameter` | Driver settings query failed (index may still work with a fixed app build) |
| `SIGSEGV` + `setPreviewDataCbRes` / `setPreviewCallbackFlag` in `camera.rk30board.so` | RK3288 driver bug when app requests **preview frame callbacks** — kills **mediaserver** | **COUNT** uses **native `/dev/video7–10`** (no callbacks). **Camera** tab = Android preview-only. **YOLO** = Native + Tray for NCNN |
| `CAMERA_ERROR_SERVER_DIED` / `error 100` | Camera service crashed — often **switching too fast**, bad resolution, or preview callbacks on RK3288 |
| `Camera.open failed` | That index cannot open (busy or broken) |
| `Active Camera Clients` not empty during test | Another app holds the camera |

---

## Step-by-step: install the latest fixed APK

The COUNT tab fix may not be on the robot unless you installed the APK **after** the code was rebuilt.

```powershell
cd "d:\OneDrive - Waaree Energies Limited\Peanut_service apk file\peanut-sdk-v1.3.0\peanut-sdk-v1.3.0\SampleApp"
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Check install time:

```powershell
adb shell dumpsys package com.keenon.peanut.sample | findstr lastUpdate
```

---

## Safe test order on the robot

1. **Stop** other Keenon apps that use cameras (food delivery on tray screen).
2. Open **only** Sample App → Developer.
3. **Camera** tab: Start on **0** → wait until you see picture → wait **1 second** → tap **1**.
4. If **0** never works, try **Start** on **1** or **2** directly (do not tap 3).
5. **YOLO** tab: on **RK3288 / T10** use **Native camera** + **Tray** model (not COCO). Tap `/dev/video7`–`10` until plates are counted. Android mode = preview only.

---

## Quick checklist

- [ ] `adb devices` shows `device`
- [ ] `Active Camera Clients` is empty before test
- [ ] Reproduced error once with `adb logcat -c` then actions on robot
- [ ] Saved log file and searched for `getParameters`, `SERVER_DIED`, `CameraFragment`
- [ ] Installed latest `app-debug.apk` after build

Send the log file (or the 20 lines around the error) to pinpoint the exact failure.
