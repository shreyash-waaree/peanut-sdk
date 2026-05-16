package com.keenon.peanut.formbackend.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PromoNdjsonStore {
  private static final Path INTEREST = Path.of("submissions-interest.ndjson");
  private static final Path FEEDBACK = Path.of("submissions-feedback.ndjson");
  private final ObjectMapper mapper = new ObjectMapper();

  public synchronized void appendInterest(Object row) throws IOException {
    append(INTEREST, row);
  }

  public synchronized void appendFeedback(Object row) throws IOException {
    append(FEEDBACK, row);
  }

  private void append(Path path, Object row) throws IOException {
    String line = mapper.writeValueAsString(row) + System.lineSeparator();
    Files.write(path, line.getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

  /** Last N lines parsed (simple tail for small files). */
  public synchronized <T> List<T> readLast(Path path, Class<T> type, int limit) throws IOException {
    if (!Files.exists(path)) {
      return Collections.emptyList();
    }
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    int n = Math.min(limit, lines.size());
    int start = Math.max(0, lines.size() - n);
    List<T> out = new ArrayList<>();
    for (int i = start; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      if (line.isEmpty()) continue;
      out.add(mapper.readValue(line, type));
    }
    return out;
  }

  public List<com.keenon.peanut.formbackend.model.InterestSubmission> readLastInterest(int limit)
      throws IOException {
    return readLast(INTEREST, com.keenon.peanut.formbackend.model.InterestSubmission.class, limit);
  }

  public List<com.keenon.peanut.formbackend.model.FeedbackSubmission> readLastFeedback(int limit)
      throws IOException {
    return readLast(FEEDBACK, com.keenon.peanut.formbackend.model.FeedbackSubmission.class, limit);
  }
}
