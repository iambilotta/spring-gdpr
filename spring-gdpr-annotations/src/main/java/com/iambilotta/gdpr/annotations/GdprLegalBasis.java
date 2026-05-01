package com.iambilotta.gdpr.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the lawful basis for processing under Art. 6(1) (and Art. 9(2) for special categories).
 *
 * <p>Lawful basis MUST be present for any personal data processing. Build-time verification
 * fails if a {@link GdprPersonalData}-bearing type lacks an enclosing {@code @GdprLegalBasis}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface GdprLegalBasis {

    LawfulBasis value();

    /**
     * Optional reference to the article subsection used (e.g. "6(1)(b)" for contract).
     * If empty, derived from {@link #value()}.
     */
    String article() default "";

    /**
     * Free-form note used in ROPA + DPIA: e.g. "consent collected at signup form".
     */
    String note() default "";

    enum LawfulBasis {
        /** Art. 6(1)(a) consent of the data subject. */
        CONSENT,
        /** Art. 6(1)(b) performance of a contract. */
        CONTRACT,
        /** Art. 6(1)(c) compliance with a legal obligation. */
        LEGAL_OBLIGATION,
        /** Art. 6(1)(d) protection of vital interests. */
        VITAL_INTERESTS,
        /** Art. 6(1)(e) public interest or official authority. */
        PUBLIC_INTEREST,
        /** Art. 6(1)(f) legitimate interests pursued by the controller. */
        LEGITIMATE_INTERESTS
    }
}
