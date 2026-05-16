Vosk offline model (Indian English small, bundled in APK)

Recommended: vosk-model-small-en-in-0.4 (~36 MB) from https://alphacephei.com/vosk/models

WHERE TO COPY IN THIS PROJECT
-------------------------------
Unzip the downloaded archive and copy the folder CONTENTS so this structure exists:

  app/src/main/assets/vosk-model-small-en-in-0.4/am/
  app/src/main/assets/vosk-model-small-en-in-0.4/conf/
  app/src/main/assets/vosk-model-small-en-in-0.4/graph/
  app/src/main/assets/vosk-model-small-en-in-0.4/ivector/

The folder name "vosk-model-small-en-in-0.4" must match exactly (see VoiceFragment.VOSK_ASSET_MODEL_FOLDER).

At first run, the app copies this tree from the APK to internal storage and loads Vosk from there.

build.gradle already lists noCompress for mdl, fst, far so large binaries are not re-compressed in the APK.

OPTIONAL (not "app only")
-------------------------
You can still deploy a model under Android/data/.../files/vosk-model/ or adb push to
/data/data/<package>/files/vosk-model/ — resolution order is:
  1) assets copy (this folder)
  2) external vosk-model
  3) internal vosk-model
  4) legacy internal cache

See VOICE_FORM_EXTENSION.md for architecture notes.
