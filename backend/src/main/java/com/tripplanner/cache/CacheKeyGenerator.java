package com.tripplanner.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * CLAUDE.md 5.3 정규화 규칙: destination trim+소문자 / duration_days(날짜 아님) /
 * budget_level trim+소문자 / preferences 정렬 후 콤마 결합 / include_nearby 포함.
 */
@Component
public class CacheKeyGenerator {

    public String generate(
            String destination, int durationDays, String budgetLevel, List<String> preferences, boolean includeNearby) {
        String normalizedDestination = destination.trim().toLowerCase();
        String normalizedBudget = budgetLevel.trim().toLowerCase();
        String normalizedPreferences = preferences.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        String combined = normalizedDestination + "|" + durationDays + "|" + normalizedBudget + "|"
                + normalizedPreferences + "|" + includeNearby;

        return sha256Hex(combined);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
