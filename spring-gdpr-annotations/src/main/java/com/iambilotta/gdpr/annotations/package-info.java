/**
 * GDPR compliance annotation API for Spring Boot.
 *
 * <p>Five annotations declare GDPR semantics in source: {@link com.iambilotta.gdpr.annotations.GdprPersonalData},
 * {@link com.iambilotta.gdpr.annotations.GdprDataSubjects}, {@link com.iambilotta.gdpr.annotations.GdprLegalBasis},
 * {@link com.iambilotta.gdpr.annotations.GdprRetention}, {@link com.iambilotta.gdpr.annotations.GdprErasable}.
 *
 * <p>Read at runtime by the {@code spring-gdpr-starter} AOP advisor (audit log, retention scheduler,
 * erasure flow) and at build time by the {@code spring-gdpr-processor} APT (DPIA + ROPA generators).
 *
 * <p>This module has zero runtime dependencies. Safe to depend on from domain modules.
 */
package com.iambilotta.gdpr.annotations;
