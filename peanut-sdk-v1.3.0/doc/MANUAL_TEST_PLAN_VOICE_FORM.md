# Manual Test Plan - Voice + Form Extension (Peanut SDK Sample App v1.3.0+)

> Last updated: 2026-03-27
> Covers the tabbed UI extension: Home tab (SDK), Form tab, Voice tab.

## 0. Preconditions
1. Robot is powered on and finished booting.
2. For development via USB:
   - Set robot USB port to `DEVICE` mode (so you can use ADB).
   - Use a male-to-male dual-ended USB cable (recommended length `2~3 meters`).
3. Robot and your development PC are reachable on the same network (if backend is remote).
4. Android app has permission to record audio (granted at runtime).

## 1. Deployment Smoke Test
1. Install the APK on the robot.
2. Launch the app (`SDK Sample`).
3. Verify three tabs appear: **Home**, **Form**, **Voice**.
4. Verify no crash on startup.
5. Swipe between tabs and confirm each loads correctly.

## 2. Peanut SDK Connection Test (local vs remote)
1. Ensure the checkbox `Remote link` is set to the expected mode for your robot.
2. Observe the status line:
   - Expected: `SDK init success`
   - If failed: check that robot environment is correct and restart the app after changing the checkbox.
3. Toggle `Remote link` and restart the app (expected: SDK init should change behavior accordingly).

## 3. Voice Flow Test (Voice tab)
1. Tap the **Voice** tab.
2. Grant microphone permission when prompted.
3. Tap **Hold to Speak**.
4. Expected UI: status badge changes to `Listening...`, "Listening..." text appears below chat.
5. Say "hello".
6. Expected:
   - User bubble (blue, right-aligned) shows your spoken text.
   - Robot bubble (grey, left-aligned) shows "Hello! How can I help you today?"
   - TTS speaks the robot reply aloud.
7. Test other phrases: "how are you", "what is your name", "bye", and an unknown phrase.
   - Expected: appropriate replies per the greeting logic table in `VOICE_FORM_EXTENSION.md`.

### Voice negative cases
1. Tap mic and stay silent.
   - Expected: no crash; robot bubble shows "Sorry, I could not hear you. (No speech detected)".
2. Tap mic button rapidly multiple times.
   - Expected: no crash; each tap starts a fresh recognition session.
3. Deny microphone permission.
   - Expected: toast "Microphone permission is needed for voice chat."

## 4. Form Flow Test (Form tab)
1. Tap the **Form** tab.
2. Leave Name blank and tap **Submit Request**.
   - Expected: inline error "Name is required" on the Name field.
3. Fill in Name but leave Destination blank and submit.
   - Expected: inline error "Destination is required".
4. Fill all fields and tap **Submit Request**.
   - Expected: toast "Form submitted successfully!", result message shown below button, fields cleared.
5. Check Logcat for tag `FormFragment` to verify all field values are logged.

## 5. Backend URL Configuration Test
1. In `Backend endpoint URL`, set your REST endpoint (full URL).
2. Trigger a voice or form request.
3. Expected:
   - On valid endpoint: response appears and is spoken.
   - On invalid endpoint: status shows `Backend error: HTTP ...` or a connection error.

## 6. Secondary Display Mirror Test (optional)
1. If your robot has a secondary display, leave the app open.
2. Send any request (voice or form).
3. Expected:
   - Secondary screen shows the same response text (via `Presentation`).
4. If secondary display is not present:
   - Expected: app still works on the main screen.

## 7. Logging/Debug (if something fails)
1. Check Android Logcat for:
   - `ConversationActivity` / `SpeechInputManager` / `TtsManager`
   - `PeanutSDK` init errors
2. If backend returns non-JSON, verify BackendClient best-effort parsing behavior.

