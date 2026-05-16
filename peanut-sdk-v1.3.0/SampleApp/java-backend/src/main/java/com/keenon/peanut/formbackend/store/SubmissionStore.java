package com.keenon.peanut.formbackend.store;

import com.keenon.peanut.formbackend.dto.FormSubmitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class SubmissionStore {
    private static final Path OUT = Path.of("submissions.ndjson");
    private final ObjectMapper mapper = new ObjectMapper();

    public synchronized String append(FormSubmitRequest req) throws IOException {
        String id = UUID.randomUUID().toString();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("name", trim(req.getName()));
        row.put("phone", trim(req.getPhone()));
        row.put("destination", trim(req.getDestination()));
        row.put("message", trim(req.getMessage()));
        row.put("submittedAt", trim(req.getSubmittedAt()) == null ? Instant.now().toString() : trim(req.getSubmittedAt()));
        String line = mapper.writeValueAsString(row) + System.lineSeparator();
        Files.write(OUT, line.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return id;
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
