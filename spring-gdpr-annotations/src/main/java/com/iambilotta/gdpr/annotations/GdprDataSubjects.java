package com.iambilotta.gdpr.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the categories of data subjects whose personal data is processed
 * by the annotated type (Art. 30(1)(c)).
 *
 * <p>Examples of categories: customers, employees, prospects, suppliers, minors,
 * patients, users-eu, users-non-eu.
 *
 * <p>Surfaces in ROPA Art. 30 records and DPIA Art. 35 risk assessment.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GdprDataSubjects {

    String[] categories();
}
