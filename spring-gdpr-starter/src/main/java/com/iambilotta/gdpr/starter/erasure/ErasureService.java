package com.iambilotta.gdpr.starter.erasure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates the right-to-erasure flow (Art. 17). Iterates registered {@link ErasureHandler}
 * beans in order and reports affected counts per entity type.
 */
public class ErasureService {

    private static final Logger LOG = LoggerFactory.getLogger(ErasureService.class);

    private final List<ErasureHandler> handlers;

    public ErasureService(List<ErasureHandler> handlers) {
        List<ErasureHandler> sorted = new ArrayList<>(handlers);
        sorted.sort(Comparator.comparingInt(ErasureHandler::order));
        this.handlers = List.copyOf(sorted);
    }

    public ErasureReport eraseSubject(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be provided");
        }
        Map<String, Integer> affectedByType = new LinkedHashMap<>();
        for (ErasureHandler handler : handlers) {
            int affected = handler.erase(subjectId);
            affectedByType.put(handler.entityType(), affected);
            LOG.info(
                    "gdpr_erasure entity={} strategy={} subject={} affected={}",
                    handler.entityType(),
                    handler.strategy(),
                    subjectId,
                    affected
            );
        }
        return new ErasureReport(subjectId, affectedByType);
    }

    public List<String> registeredTypes() {
        return handlers.stream().map(ErasureHandler::entityType).toList();
    }
}
