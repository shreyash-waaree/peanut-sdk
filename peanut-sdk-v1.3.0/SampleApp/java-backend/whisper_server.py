"""
Persistent Whisper transcription sidecar.

Run once; the Spring backend POSTs each recording to 127.0.0.1:9090/transcribe
so the model stays loaded and short utterances complete in ~200-600 ms instead
of the 5-10 s per-request cold start of the original subprocess mode.

Start:
    python -m pip install -r requirements-whisper.txt
    python whisper_server.py              # defaults: base.en, 9090

Env vars:
    WHISPER_MODEL   model size/name (default "base.en"; "tiny.en" is ~2x faster,
                    "small.en" is more accurate)
    WHISPER_PORT    listen port (default 9090)
    WHISPER_DEVICE  "cpu" (default) or "cuda"
    WHISPER_COMPUTE "int8" (default), "int8_float16", "float16", "float32"
"""

from __future__ import annotations

import io
import logging
import os
import tempfile
import time
from pathlib import Path

from fastapi import FastAPI, HTTPException, Request, UploadFile, File
from fastapi.responses import JSONResponse

try:
    from faster_whisper import WhisperModel
except Exception as e:  # pragma: no cover
    raise SystemExit(
        "faster-whisper is not installed. Run:\n"
        "  python -m pip install -r requirements-whisper.txt\n"
        f"Original error: {e}"
    )

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)
log = logging.getLogger("whisper-server")

MODEL_NAME = os.environ.get("WHISPER_MODEL", "base.en")
DEVICE = os.environ.get("WHISPER_DEVICE", "cpu")
COMPUTE = os.environ.get("WHISPER_COMPUTE", "int8")
PORT = int(os.environ.get("WHISPER_PORT", "9090"))

log.info("Loading Whisper model=%s device=%s compute=%s ...", MODEL_NAME, DEVICE, COMPUTE)
_t0 = time.time()
MODEL = WhisperModel(MODEL_NAME, device=DEVICE, compute_type=COMPUTE)
log.info("Model loaded in %.2fs", time.time() - _t0)

app = FastAPI(title="Whisper Sidecar", version="1.0")


def _transcribe_bytes(wav_bytes: bytes) -> dict:
    if not wav_bytes:
        raise HTTPException(status_code=400, detail="empty body")
    # faster-whisper accepts a filepath or a file-like; on Windows temp files
    # need to be closed before re-open, so write then transcribe then delete.
    tmp = tempfile.NamedTemporaryFile(prefix="peanut-whisper-", suffix=".wav", delete=False)
    tmp_path = Path(tmp.name)
    try:
        tmp.write(wav_bytes)
        tmp.flush()
        tmp.close()
        t0 = time.time()
        segments, info = MODEL.transcribe(
            str(tmp_path),
            language="en",
            task="transcribe",
            beam_size=1,
            best_of=1,
            temperature=0.0,
            # Mobile side already VADs; server VAD would add ~200-500 ms.
            vad_filter=False,
            without_timestamps=True,
            condition_on_previous_text=False,
            no_speech_threshold=0.55,
        )
        text = " ".join(seg.text.strip() for seg in segments).strip()
        dur = time.time() - t0
        log.info(
            "transcribe bytes=%d duration=%.2fs text=%r lang=%s",
            len(wav_bytes),
            dur,
            text[:80],
            getattr(info, "language", "?"),
        )
        return {"text": text, "durationSec": round(dur, 3)}
    finally:
        try:
            tmp_path.unlink(missing_ok=True)
        except Exception:
            pass


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "model": MODEL_NAME, "device": DEVICE, "compute": COMPUTE}


@app.post("/transcribe")
async def transcribe(request: Request):
    """
    Accepts either:
      * application/octet-stream — raw WAV bytes in body (preferred, zero copies)
      * multipart/form-data      — field 'audio' with the WAV file
    """
    ctype = (request.headers.get("content-type") or "").lower()
    if ctype.startswith("multipart/"):
        form = await request.form()
        up = form.get("audio")
        if up is None or not isinstance(up, UploadFile):
            return JSONResponse(
                status_code=400,
                content={"status": "error", "error": "missing 'audio' field"},
            )
        body = await up.read()
    else:
        body = await request.body()
    result = _transcribe_bytes(body)
    return {"status": "ok", **result}


if __name__ == "__main__":
    import uvicorn

    # Bind to loopback: this is an in-process helper for the Spring backend.
    uvicorn.run(app, host="127.0.0.1", port=PORT, log_level="info")
