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
import com.iambilotta.gdpr.annotations.GdprLegalBasis.Art10Basis;
import com.iambilotta.gdpr.annotations.GdprLegalBasis.Art9Condition;
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

        boolean specialCategory = isAnyAnnotationSpecialCategory(method, declaring);

        GdprLegalBasis legalBasisAnnotation = pickLegalBasis(method, declaring);
        String legalBasisRef = formatLegalBasis(legalBasisAnnotation, specialCategory);

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

    /**
     * Checks every {@link GdprPersonalData} annotation reachable from the call site
     * (method, declaring type, parameters) and returns true if ANY of them flags
     * special category. Aggregation, not first-match, so a method that touches a
     * non-special parameter and a special parameter is still flagged correctly.
     */
    private static boolean isAnyAnnotationSpecialCategory(Method method, Class<?> declaring) {
        GdprPersonalData fromMethod = method.getAnnotation(GdprPersonalData.class);
        if (fromMethod != null && fromMethod.specialCategory()) {
            return true;
        }
        GdprPersonalData fromType = declaring.getAnnotation(GdprPersonalData.class);
        if (fromType != null && fromType.specialCategory()) {
            return true;
        }
        for (Parameter parameter : method.getParameters()) {
            GdprPersonalData fromParam = parameter.getAnnotation(GdprPersonalData.class);
            if (fromParam != null && fromParam.specialCategory()) {
                return true;
            }
        }
        return false;
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

    /**
     * Maps the lawful basis to its article reference. Composite when special-category
     * applies: Art. 6(1)(*) AND Art. 9(2)(*) (or Art. 10) MUST coexist for valid
     * special-category processing. The composite article string ("6(1)(b) + 9(2)(h)")
     * is what regulators expect to see in the audit row.
     */
    static String formatLegalBasis(GdprLegalBasis basis, boolean specialCategory) {
        if (basis == null) {
            return null;
        }
        String art6 = basis.article() != null && !basis.article().isBlank()
                ? basis.article()
                : mapArt6(basis.value());
        if (!specialCategory) {
            return art6;
        }
        String art9 = mapArt9(basis.specialBasis());
        if (art9 != null) {
            return art6 + " + " + art9;
        }
        String art10 = mapArt10(basis.criminalBasis());
        if (art10 != null) {
            return art6 + " + " + art10;
        }
        return art6 + " + Art.9/10 MISSING";
    }

    private static String mapArt6(GdprLegalBasis.LawfulBasis value) {
        return switch (value) {
            case CONSENT -> "6(1)(a)";
            case CONTRACT -> "6(1)(b)";
            case LEGAL_OBLIGATION -> "6(1)(c)";
            case VITAL_INTERESTS -> "6(1)(d)";
            case PUBLIC_INTEREST -> "6(1)(e)";
            case LEGITIMATE_INTERESTS -> "6(1)(f)";
        };
    }

    private static String mapArt9(Art9Condition condition) {
        return switch (condition) {
            case NONE -> null;
            case EXPLICIT_CONSENT -> "9(2)(a)";
            case EMPLOYMENT_LAW -> "9(2)(b)";
            case VITAL_INTERESTS -> "9(2)(c)";
            case NON_PROFIT -> "9(2)(d)";
            case PUBLICLY_DISCLOSED -> "9(2)(e)";
            case LEGAL_CLAIMS -> "9(2)(f)";
            case SUBSTANTIAL_PUBLIC_INTEREST -> "9(2)(g)";
            case PREVENTIVE_MEDICINE -> "9(2)(h)";
            case PUBLIC_HEALTH -> "9(2)(i)";
            case ARCHIVE_PURPOSES -> "9(2)(j)";
        };
    }

    private static String mapArt10(Art10Basis basis) {
        return switch (basis) {
            case NONE -> null;
            case AUTHORISED_BY_LAW -> "10 (authorised by law)";
            case OFFICIAL_AUTHORITY -> "10 (official authority)";
        };
    }
}
