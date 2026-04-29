package com.iambilotta.gdpr.starter.audit;

/**
 * Resolves the current principal name (Spring Security {@code Authentication#getName()},
 * a request header, or any custom mechanism). Default implementation returns {@code "system"}.
 *
 * <p>Override by providing a {@code @Bean} of this type.
 */
@FunctionalInterface
public interface ActorResolver {

    String currentActor();

    static ActorResolver fixed(String actor) {
        return () -> actor;
    }
}
