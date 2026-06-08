package com.iambilotta.gdpr.starter.web;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The starter mounts {@code /gdpr/**} open by default (erasure deletes data). When the surface is
 * mounted, a startup WARN must name the foot-gun and the fix, so an adopter who forgot to wire
 * Spring Security sees it loudly instead of shipping an open erasure endpoint silently.
 */
class GdprSecurityWarningTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(GdprSecurityWarning.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    /**
     * @spec.given the GDPR web surface is about to be mounted at a base path
     * @spec.when  the startup security warning runs
     * @spec.then  a single WARN names the open base path and tells the adopter to wire Spring Security
     * @spec.us    US-DX-004-secure-by-default-signal
     */
    @Test
    void warnsThatTheGdprSurfaceIsOpenByDefault() {
        new GdprSecurityWarning("/gdpr").warnIfUnsecured();

        List<ILoggingEvent> warnings = appender.list.stream()
                .filter(e -> e.getLevel() == Level.WARN)
                .toList();

        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0).getFormattedMessage())
                .contains("/gdpr/**")
                .containsIgnoringCase("authentication");
    }
}
