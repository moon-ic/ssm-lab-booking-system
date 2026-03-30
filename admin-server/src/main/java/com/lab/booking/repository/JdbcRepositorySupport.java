package com.lab.booking.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class JdbcRepositorySupport {

    private static final Charset GBK = Charset.forName("GBK");

    protected final ObjectMapper objectMapper;

    protected JdbcRepositorySupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize json column", ex);
        }
    }

    protected List<Long> toLongList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() { });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize long list column", ex);
        }
    }

    protected Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize map column", ex);
        }
    }

    protected LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    protected String normalizeLegacyText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String best = value;
        int bestScore = scoreText(value);
        for (String candidate : buildCandidates(value)) {
            int candidateScore = scoreText(candidate);
            if (candidateScore > bestScore) {
                best = candidate;
                bestScore = candidateScore;
            }
        }
        return best;
    }

    private List<String> buildCandidates(String value) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(value);
        candidates.add(recode(value, GBK, StandardCharsets.UTF_8));
        candidates.add(recode(value, StandardCharsets.UTF_8, GBK));

        List<String> snapshot = new ArrayList<>(candidates);
        for (String candidate : snapshot) {
            candidates.add(recode(candidate, GBK, StandardCharsets.UTF_8));
            candidates.add(recode(candidate, StandardCharsets.UTF_8, GBK));
        }
        return new ArrayList<>(candidates);
    }

    private String recode(String value, Charset source, Charset target) {
        return new String(value.getBytes(source), target);
    }

    private int scoreText(String value) {
        int score = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (isReadableCjk(ch)) {
                score += 4;
            } else if (isCommonReadableChar(ch)) {
                score += 1;
            } else if (isSuspiciousChar(ch)) {
                score -= 6;
            } else {
                score -= 1;
            }
        }
        return score;
    }

    private boolean isReadableCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private boolean isCommonReadableChar(char ch) {
        return Character.isLetterOrDigit(ch)
                || Character.isWhitespace(ch)
                || "-_/.:,()[]{}+#".indexOf(ch) >= 0;
    }

    private boolean isSuspiciousChar(char ch) {
        return ch == '\uFFFD'
                || ch == '?'
                || Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.BOX_DRAWING
                || Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.GEOMETRIC_SHAPES;
    }
}
