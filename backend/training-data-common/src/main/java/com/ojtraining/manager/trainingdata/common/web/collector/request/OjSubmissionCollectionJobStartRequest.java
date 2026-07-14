package com.ojtraining.manager.trainingdata.common.web.collector.request;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record OjSubmissionCollectionJobStartRequest(
        List<String> usernames,
        Long lookbackHours,
        Map<String, Long> lookbackHoursByUsername,
        Boolean refreshWarehouse,
        String ojName
) {
    public List<String> requireUsernames() {
        if (usernames == null || usernames.isEmpty()) {
            throw new IllegalArgumentException("usernames must not be empty");
        }
        return usernames;
    }

    public Duration requireLookbackDuration() {
        return requireLookbackDuration(lookbackHours, "lookbackHours");
    }

    public Map<String, Duration> requireLookbackDurationsByUsername() {
        if (lookbackHoursByUsername == null || lookbackHoursByUsername.isEmpty()) {
            return Map.of();
        }
        Map<String, Duration> durations = new LinkedHashMap<>();
        lookbackHoursByUsername.forEach((username, hours) -> {
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("lookbackHoursByUsername username must not be blank");
            }
            durations.put(
                    username.trim(),
                    requireLookbackDuration(hours, "lookbackHoursByUsername[" + username.trim() + "]")
            );
        });
        return Map.copyOf(durations);
    }

    private static Duration requireLookbackDuration(Long hours, String fieldName) {
        if (hours == null || hours < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        try {
            return Duration.ofHours(hours);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(fieldName + " is too large", ex);
        }
    }

    public boolean refreshWarehouseOrDefault() {
        return Boolean.TRUE.equals(refreshWarehouse);
    }

    public String optionalOjName() {
        return ojName == null || ojName.isBlank() ? null : ojName.trim();
    }
}
