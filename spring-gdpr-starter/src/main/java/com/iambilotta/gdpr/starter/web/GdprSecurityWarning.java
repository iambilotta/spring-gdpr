package com.iambilotta.gdpr.starter.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Startup poka-yoke for the open-by-default web surface. The starter mounts {@code <base-path>/**}
 * with no authentication of its own (erasure deletes data, the access export reveals a subject's
 * dossier), so it emits one loud WARN at startup naming the exposed path and the fix.
 *
 * <p>Fail-soft, not fail-fast: the app still boots (good DX), but the silent foot-gun becomes a
 * visible signal. The library cannot know whether the host wired a {@code SecurityFilterChain} over
 * the path (Spring Security may be absent from the classpath entirely), so it warns whenever the
 * surface is mounted rather than guessing; an adopter who has secured the path can raise the
 * {@code com.iambilotta.gdpr.starter.web.GdprSecurityWarning} logger to {@code ERROR} to silence it.
 */
public class GdprSecurityWarning implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(GdprSecurityWarning.class);

    private final String basePath;

    public GdprSecurityWarning(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public void afterPropertiesSet() {
        warnIfUnsecured();
    }

    /**
     * Emits the single startup WARN. Public so it is unit-testable in isolation; normally invoked
     * once by {@link #afterPropertiesSet()} when the bean is created.
     */
    public void warnIfUnsecured() {
        LOG.warn(
                "spring-gdpr mounted its REST surface at {}/** WITHOUT authentication. "
                        + "These endpoints delete personal data (DELETE {}/erasure/{{subjectId}}) and "
                        + "reveal a subject's dossier (GET {}/access/export). Wire a SecurityFilterChain "
                        + "restricting {}/** to a privileged role (e.g. DPO) and override the ActorResolver "
                        + "bean so audit rows carry the real principal instead of \"system\". "
                        + "See the README \"Wiring with Spring Security\" section.",
                basePath, basePath, basePath, basePath);
    }
}
