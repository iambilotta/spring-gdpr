package com.iambilotta.gdpr.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the lawful basis for processing.
 *
 * <p>For ordinary personal data: {@link #value()} maps to Art. 6(1)(a..f). The audit log
 * and the ROPA record use the corresponding article reference.
 *
 * <p>For Art. 9 special-category data (race, religion, health, biometric,
 * sex life, trade-union membership, political opinions, genetic data): set
 * {@link #specialBasis()} to a non-{@code NONE} {@link Art9Condition}. Art. 9 requires
 * BOTH a lawful basis under Art. 6 AND a condition under Art. 9(2), so {@link #value()}
 * remains relevant.
 *
 * <p>For Art. 10 data (criminal convictions): set {@link #criminalBasis()}. Same
 * dual-basis logic.
 *
 * <p>If a member is annotated {@code @GdprPersonalData(specialCategory = true)} but the
 * enclosing {@code @GdprLegalBasis} declares {@link Art9Condition#NONE}, the audit log
 * still records the Art. 6 basis but flags the row as compliance-incomplete; the build-time
 * processor emits a WARNING in this case.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface GdprLegalBasis {

    LawfulBasis value();

    /**
     * Optional override for {@link #value()}. Useful when the article reference includes
     * a sub-clause not captured by the enum (e.g. "6(1)(b) read with national law X").
     */
    String article() default "";

    /**
     * Free-form note used in ROPA + DPIA: e.g. "consent collected at signup form".
     */
    String note() default "";

    /**
     * Art. 9(2) condition for special-category data. {@link Art9Condition#NONE} means the
     * data processed under this basis is NOT Art. 9.
     */
    Art9Condition specialBasis() default Art9Condition.NONE;

    /**
     * Art. 10 condition for criminal-convictions data. {@link Art10Basis#NONE} means the
     * data processed under this basis is NOT Art. 10.
     */
    Art10Basis criminalBasis() default Art10Basis.NONE;

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

    enum Art9Condition {
        NONE,
        /** Art. 9(2)(a) explicit consent. */
        EXPLICIT_CONSENT,
        /** Art. 9(2)(b) employment, social security, or social protection law. */
        EMPLOYMENT_LAW,
        /** Art. 9(2)(c) vital interests (subject physically or legally incapable). */
        VITAL_INTERESTS,
        /** Art. 9(2)(d) processing by a not-for-profit body. */
        NON_PROFIT,
        /** Art. 9(2)(e) data manifestly made public by the data subject. */
        PUBLICLY_DISCLOSED,
        /** Art. 9(2)(f) establishment, exercise, or defence of legal claims. */
        LEGAL_CLAIMS,
        /** Art. 9(2)(g) substantial public interest. */
        SUBSTANTIAL_PUBLIC_INTEREST,
        /** Art. 9(2)(h) preventive or occupational medicine, medical diagnosis. */
        PREVENTIVE_MEDICINE,
        /** Art. 9(2)(i) public interest in the area of public health. */
        PUBLIC_HEALTH,
        /** Art. 9(2)(j) archiving purposes, scientific or historical research, statistics. */
        ARCHIVE_PURPOSES
    }

    enum Art10Basis {
        NONE,
        /** Authorised by Union or member-state law providing appropriate safeguards. */
        AUTHORISED_BY_LAW,
        /** Carried out under the control of an official authority. */
        OFFICIAL_AUTHORITY
    }
}
