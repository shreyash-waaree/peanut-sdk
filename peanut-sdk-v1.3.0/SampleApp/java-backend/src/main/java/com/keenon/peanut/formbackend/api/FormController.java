package com.keenon.peanut.formbackend.api;

import com.keenon.peanut.formbackend.dto.FormSubmitRequest;
import com.keenon.peanut.formbackend.store.SubmissionStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class FormController {
    private final SubmissionStore store;

    public FormController(SubmissionStore store) {
        this.store = store;
    }

    @GetMapping("health")
    public Map<String, String> health() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("status", "ok");
        out.put("service", "form-backend");
        return out;
    }

    @PostMapping("form-submit")
    public ResponseEntity<Map<String, String>> submit(@RequestBody FormSubmitRequest req) {
        if (blank(req.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (blank(req.getDestination())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "destination is required");
        }
        try {
            String id = store.append(req);
            Map<String, String> out = new LinkedHashMap<>();
            out.put("status", "ok");
            out.put("id", id);
            return ResponseEntity.ok(out);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "could not store submission", e);
        }
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
