package com.keenon.peanut.formbackend.api;

import com.keenon.peanut.formbackend.speech.SpeechTranscribeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SpeechTranscribeController {

    private final SpeechTranscribeService speechTranscribeService;

    public SpeechTranscribeController(SpeechTranscribeService speechTranscribeService) {
        this.speechTranscribeService = speechTranscribeService;
    }

    /**
     * Multipart field name: {@code audio} — RIFF WAV, PCM 16-bit mono (e.g. 16 kHz from Android).
     */
    @PostMapping("speech-transcribe")
    public ResponseEntity<Map<String, String>> transcribe(@RequestParam("audio") MultipartFile audio) {
        Map<String, String> out = new LinkedHashMap<>();
        if (audio == null || audio.isEmpty()) {
            out.put("status", "error");
            out.put("error", "missing audio");
            return ResponseEntity.badRequest().body(out);
        }
        if (!speechTranscribeService.isEnabled()) {
            out.put("status", "error");
            out.put("error", "Speech API disabled on server (speech.whisper.enabled=false)");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(out);
        }
        try {
            byte[] bytes = audio.getBytes();
            String text = speechTranscribeService.transcribeWav(bytes);
            out.put("status", "ok");
            out.put("text", text == null ? "" : text);
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException e) {
            out.put("status", "error");
            out.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(out);
        } catch (IOException e) {
            out.put("status", "error");
            out.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(out);
        }
    }
}
