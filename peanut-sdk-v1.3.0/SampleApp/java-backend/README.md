# Spring Boot Form Backend (LAN)

This backend receives form submissions from the Android `FormFragment`.

## Endpoints

- `GET /health` -> `{"status":"ok","service":"form-backend"}`
- `POST /form-submit`
- `POST /speech-transcribe` — multipart field **`audio`**: RIFF WAV, **PCM 16-bit mono** (e.g. 16 kHz from the robot). Returns JSON `{"status":"ok","text":"..."}` or `{"status":"error","error":"..."}`. Uses a **local Whisper command** on the PC (not on the robot).

Request JSON:

```json
{
  "name": "John",
  "phone": "9999999999",
  "destination": "Room 203",
  "message": "Water bottle",
  "submittedAt": "1712500000000"
}
```

Response JSON:

```json
{
  "status": "ok",
  "id": "uuid-value"
}
```

Each valid submit is appended to `submissions.ndjson`.

### Local Whisper setup (for `/speech-transcribe`)

**Use the persistent sidecar** — the model stays loaded, so each recording
transcribes in ~200-600 ms instead of the 5-10 s per-request cold start you get
if you spawn a fresh `python transcribe_whisper.py` every time.

1. Install Python + dependencies on the backend laptop:

```powershell
python -m pip install -r requirements-whisper.txt
```

2. **Start the sidecar once** (keep it running alongside the Spring Boot app):

```powershell
python whisper_server.py
```

   Environment variables (optional):
   - `WHISPER_MODEL` — default `base.en`. Use `tiny.en` for ~2× faster, slightly less accurate; `small.en` for more accurate, slower.
   - `WHISPER_PORT` — default `9090`.
   - `WHISPER_DEVICE` — `cpu` (default) or `cuda` if you have a GPU.
   - `WHISPER_COMPUTE` — `int8` (default), or `float16` on GPU.

3. Start the Spring Boot backend:

```powershell
mvn spring-boot:run
```

The backend auto-discovers the sidecar via
`speech.whisper.sidecar-url=http://127.0.0.1:9090/transcribe` in
`application.properties`. If the sidecar is unreachable, it falls back to the
slow subprocess mode (`speech.whisper.command=python,transcribe_whisper.py,{audio}`).

4. Confirm it's running:

```powershell
curl http://127.0.0.1:9090/health
```

**Typical latency (short utterance, CPU int8):**
- Sidecar + `tiny.en`   ≈ 150-400 ms
- Sidecar + `base.en`   ≈ 300-700 ms  *(default, best quality/speed balance)*
- Subprocess + `base`   ≈ 5-10 s      *(fallback only — model reloads per call)*

The Android app posts to the same **BASE_URL** as the form (`BackendConfig.SPEECH_TRANSCRIBE_URL` = `{BASE_URL}/speech-transcribe`) when the robot has **network** but **no** system `SpeechRecognizer`.

## Run

From `SampleApp/java-backend`:

```powershell
mvn spring-boot:run
```

Health test:

```powershell
curl http://localhost:8080/health
```

## LAN usage

Android app can use either:

- Runtime entry in Form tab (**Backend IP / URL** field, recommended for changing networks)
- Or fallback default in `app/src/main/java/com/keenon/peanut/sample/BackendConfig.java`

For fallback default, update `BASE_URL` to your PC IP, e.g.:

`http://192.168.1.50:8080`
