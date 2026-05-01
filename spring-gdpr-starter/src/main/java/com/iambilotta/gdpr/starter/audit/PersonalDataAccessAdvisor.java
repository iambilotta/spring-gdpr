package com.iambilotta.gdpr.starter.audit;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iambilotta.gdpr.annotations.GdprLegalBasis;
import com.iambilotta.gdpr.annotations.GdprPersonalData;

/**
 * Captures every call to a method that bears (directly, on a parameter, on the return type,
 * or on the declaring class) the {@link GdprPersonalData} annotation, and emits one
 * {@link AuditAccessRecord} to the configured {@link AuditSink}.
 *
 * <p>Pointcut is bounded to {@code @GdprPersonalData} to avoid auditing every method on the
 * classpath. The user is responsible for placing the annotation on the right boundary
 * (typically repository methods, service-layer accessors, controllers returning DTOs).
 */
@Aspect
public class PersonalDataAccessAdvisor {

    private static final Logger LOG = LoggerFactory.getLogger(PersonalDataAccessAdvisor.class);

    private final AuditSink sink;
    private final ActorResolver actorResolver;
    private final SubjectIdResolver subjectIdResolver;

    public PersonalDataAccessAdvisor(AuditSink sink, ActorResolver actorResolver, SubjectIdResolver subjectIdResolver) {
        this.sink = sink;
        this.actorResolver = actorResolver;
        this.subjectIdResolver = subjectIdResolver;
    }

    @Before(
            "@annotation(com.iambilotta.gdpr.annotations.GdprPersonalData)"
                    + " || @within(com.iambilotta.gdpr.annotations.GdprPersonalData)"
                    + " || execution(* *(.., @com.iambilotta.gdpr.annotations.GdprPersonalData (*), ..))"
    )
    public void capture(JoinPoint jp) {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        Class<?> declaring = method.getDeclaringClass();

        GdprPersonalData personalData = pickPersonalData(method, declaring);
        boolean specialCategory = personalData != null && personalData.specialCategory();

        GdprLegalBasis legalBasisAnnotation = pickLegalBasis(method, declaring);
        String legalBasisRef = formatLegalBasis(legalBasisAnnotation);

        AuditAccessRecord record = new AuditAccessRecord(
                UUID.randomUUID().toString(),
                Instant.now(),
                actorResolver.currentActor(),
                subjectIdResolver.resolve(method, jp.getArgs()),
                declaring.getName(),
                method.getName(),
                legalBasisRef,
                specialCategory
        );
        try {
            sink.write(record);
        } catch (RuntimeException ex) {
            LOG.error(
                    "spring-gdpr audit sink threw on write for event_id={} type={} member={}: {}. "
                            + "Business method continues. Wrap your sink with AsyncAuditSinkDecorator to absorb sink failures off the request thread.",
                    record.eventId(), record.targetType(), record.targetMember(), ex.toString());
        }
    }

    private static GdprPersonalData pickPersonalData(Method method, Class<?> declaring) {
        GdprPersonalData fromMethod = method.getAnnotation(GdprPersonalData.class);
        if (fromMethod != null) {
            return fromMethod;
        }
        GdprPersonalData fromType = declaring.getAnnotation(GdprPersonalData.class);
        if (fromType != null) {
            return fromType;
        }
        for (Parameter parameter : method.getParameters()) {
            GdprPersonalData fromParam = parameter.getAnnotation(GdprPersonalData.class);
            if (fromParam != null) {
                return fromParam;
            }
        }
        return null;
    }

    private static GdprLegalBasis pickLegalBasis(AnnotatedElement... candidates) {
        for (AnnotatedElement element : candidates) {
            GdprLegalBasis basis = element.getAnnotation(GdprLegalBasis.class);
            if (basis != null) {
                return basis;
            }
        }
        return null;
    }

    private static String formatLegalBasis(GdprLegalBasis basis) {
        if (basis == null) {
            return null;
        }
        if (basis.article() != null && !basis.article().isBlank()) {
            return basis.article();
        }
        return switch (basis.value()) {
            case CONSENT -> "6(1)(a)";
            case CONTRACT -> "6(1)(b)";
            case LEGAL_OBLIGATION -> "6(1)(c)";
            case VITAL_INTERESTS -> "6(1)(d)";
            case PUBLIC_INTEREST -> "6(1)(e)";
            case LEGITIMATE_INTERESTS -> "6(1)(f)";
        };
    }
}
