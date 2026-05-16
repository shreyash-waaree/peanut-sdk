import sys


def main():
    if len(sys.argv) < 2:
        print("usage: python transcribe_whisper.py <audio.wav>", file=sys.stderr)
        return 2
    audio_path = sys.argv[1]
    try:
        # Requires: pip install faster-whisper
        from faster_whisper import WhisperModel
    except Exception as e:
        print(f"faster-whisper import error: {e}", file=sys.stderr)
        return 3

    model_name = "base"
    model = WhisperModel(model_name, device="cpu", compute_type="int8")
    segments, _ = model.transcribe(audio_path, language="en", vad_filter=True)
    text = " ".join([seg.text.strip() for seg in segments]).strip()
    print(text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
