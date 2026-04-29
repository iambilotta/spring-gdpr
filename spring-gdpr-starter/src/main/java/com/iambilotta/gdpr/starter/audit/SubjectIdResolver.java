package com.iambilotta.gdpr.starter.audit;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Extracts a subject id from the call site, when one is identifiable. Default looks for
 * a parameter named {@code subjectId} (case-insensitive) and stringifies it.
 *
 * <p>Override by providing a {@code @Bean} of this type. A custom resolver is the right
 * place to read MDC, the security context, or a tenant header.
 */
@FunctionalInterface
public interface SubjectIdResolver {

    String resolve(Method method, Object[] args);

    static SubjectIdResolver byParameterName() {
        return (method, args) -> {
            if (args == null || args.length == 0) {
                return null;
            }
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                String name = parameters[i].getName();
                if (name != null && name.equalsIgnoreCase("subjectId") && args[i] != null) {
                    return String.valueOf(args[i]);
                }
            }
            return null;
        };
    }
}
