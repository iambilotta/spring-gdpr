package com.iambilotta.gdpr.starter.erasure;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Orchestrates the right-to-erasure flow (Art. 17). Iterates registered {@link ErasureHandler}
 * beans in order and reports affected counts per entity type.
 *
 * <p>After the report is assembled (every handler committed), it notifies the post-erasure
 * extension points (issue #37): each registered {@link ErasureListener} is invoked once with the
 * report and, when an {@link ApplicationEventPublisher} is present, a {@link SubjectErasedEvent} is
 * published. This is the deterministic signal an event-sourced / CQRS consumer needs to rebuild a
 * projection that resolved a now-dangling forgettable-payload reference (ADR-0010). Both paths are
 * optional and backward compatible: with no listener and no publisher this is a no-op.
 */
public class ErasureService {

    private static final Logger LOG = LoggerFactory.getLogger(ErasureService.class);

    private final List<ErasureHandler> handlers;
    private final List<ErasureListener> listeners;
    private final ApplicationEventPublisher eventPublisher;

    public ErasureService(List<ErasureHandler> handlers) {
        this(handlers, List.of(), null);
    }

    public ErasureService(List<ErasureHandler> handlers, List<ErasureListener> listeners) {
        this(handlers, listeners, null);
    }

    public ErasureService(
            List<ErasureHandler> handlers,
            List<ErasureListener> listeners,
            ApplicationEventPublisher eventPublisher) {
        List<ErasureHandler> sorted = new ArrayList<>(handlers);
        sorted.sort(Comparator.comparingInt(ErasureHandler::order));
        this.handlers = List.copyOf(sorted);
        this.listeners = List.copyOf(listeners);
        this.eventPublisher = eventPublisher;
    }

    public ErasureReport eraseSubject(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be provided");
        }
        Map<String, Integer> affectedByType = new LinkedHashMap<>();
        for (ErasureHandler handler : handlers) {
            int affected = handler.erase(subjectId);
            String entityFqn = handler.entityType().getName();
            affectedByType.put(entityFqn, affected);
            LOG.info(
                    "gdpr_erasure entity={} strategy={} subject={} affected={}",
                    entityFqn,
                    handler.strategy(),
                    subjectId,
                    affected
            );
        }
        ErasureReport report = new ErasureReport(subjectId, affectedByType);
        notifyAfterErasure(report);
        return report;
    }

    /**
     * Fires the post-erasure extension points once the erasure has committed. The handlers already
     * ran, so a listener fault never un-erases the subject: every listener still runs, then the first
     * failure is surfaced as an {@link ErasureListenerException} (logged, never swallowed).
     */
    private void notifyAfterErasure(ErasureReport report) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(
                    new SubjectErasedEvent(report.subjectId(), report.affectedByType(), Instant.now()));
        }
        ErasureListenerException surfaced = null;
        for (ErasureListener listener : listeners) {
            try {
                listener.onSubjectErased(report);
            } catch (RuntimeException ex) {
                LOG.error(
                        "gdpr_erasure_listener_failed subject={} listener={}",
                        report.subjectId(),
                        listener.getClass().getName(),
                        ex);
                if (surfaced == null) {
                    surfaced = new ErasureListenerException(report.subjectId(), ex);
                } else {
                    surfaced.addSuppressed(ex);
                }
            }
        }
        if (surfaced != null) {
            throw surfaced;
        }
    }

    public List<String> registeredTypes() {
        return handlers.stream().map(h -> h.entityType().getName()).toList();
    }
}
