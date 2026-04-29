package com.iambilotta.gdpr.starter.erasure;

import java.util.Map;

public record ErasureReport(String subjectId, Map<String, Integer> affectedByType) {

    public int totalAffected() {
        return affectedByType.values().stream().mapToInt(Integer::intValue).sum();
    }
}
