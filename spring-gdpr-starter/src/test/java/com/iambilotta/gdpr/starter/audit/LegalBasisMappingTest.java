package com.iambilotta.gdpr.starter.audit;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.Test;

import com.iambilotta.gdpr.annotations.GdprLegalBasis;
import com.iambilotta.gdpr.annotations.GdprLegalBasis.Art10Basis;
import com.iambilotta.gdpr.annotations.GdprLegalBasis.Art9Condition;
import com.iambilotta.gdpr.annotations.GdprLegalBasis.LawfulBasis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the static {@link PersonalDataAccessAdvisor#formatLegalBasis(GdprLegalBasis, boolean)}.
 * Verifies the article-reference mapping is regulation-correct for ordinary, special-category,
 * and criminal-convictions data.
 */
class LegalBasisMappingTest {

    @Test
    void ordinaryDataReturnsArticle6Reference() {
        assertThat(PersonalDataAccessAdvisor.formatLegalBasis(
                fakeBasis(LawfulBasis.CONTRACT, "", Art9Condition.NONE, Art10Basis.NONE), false))
                .isEqualTo("6(1)(b)");
        assertThat(PersonalDataAccessAdvisor.formatLegalBasis(
                fakeBasis(LawfulBasis.CONSENT, "", Art9Condition.NONE, Art10Basis.NONE), false))
                .isEqualTo("6(1)(a)");
    }

    @Test
    void explicitArticleOverrideWins() {
        assertThat(PersonalDataAccessAdvisor.formatLegalBasis(
                fakeBasis(LawfulBasis.CONTRACT, "6(1)(b) + national art X", Art9Condition.NONE, Art10Basis.NONE), false))
                .isEqualTo("6(1)(b) + national art X");
    }

    @Test
    void specialCategoryWithArt9ProducesCompositeReference() {
        assertThat(PersonalDataAccessAdvisor.formatLegalBasis(
                fakeBasis(LawfulBasis.CONSENT, "", Art9Condition.EXPLICIT_CONSENT, Art10Basis.NONE), true))
                .isEqualTo("6(1)(a) + 9(2)(a)");

        assertThat(PersonalDataAccessAdvisor.formatLegalBasis(
                fakeBasis(LawfulBasis.LEGAL_OBLIGATION, "", Art9Condition.PREVENTIVE_MEDICINE, Art10Basis.NONE), true))
                .isEqualTo("6(1)(c) + 9(2)(h)");
    }

    @Test
    void criminalConvictionsProducesArt10Reference() {
        assertThat(PersonalDataAccessAdvisor.formatLegalBasis(
                fakeBasis(LawfulBasis.LEGAL_OBLIGATION, "", Art9Condition.NONE, Art10Basis.AUTHORISED_BY_LAW), true))
                .isEqualTo("6(1)(c) + 10 (authorised by law)");
    }

    @Test
    void specialCategoryWithoutArt9OrArt10FlagsMissing() {
        assertThat(PersonalDataAccessAdvisor.formatLegalBasis(
                fakeBasis(LawfulBasis.CONSENT, "", Art9Condition.NONE, Art10Basis.NONE), true))
                .isEqualTo("6(1)(a) + Art.9/10 MISSING");
    }

    @Test
    void nullAnnotationReturnsNull() {
        assertThat(PersonalDataAccessAdvisor.formatLegalBasis(null, false)).isNull();
        assertThat(PersonalDataAccessAdvisor.formatLegalBasis(null, true)).isNull();
    }

    private static GdprLegalBasis fakeBasis(
            LawfulBasis value, String article, Art9Condition special, Art10Basis criminal) {
        return new GdprLegalBasis() {
            @Override public Class<? extends Annotation> annotationType() { return GdprLegalBasis.class; }
            @Override public LawfulBasis value() { return value; }
            @Override public String article() { return article; }
            @Override public String note() { return ""; }
            @Override public Art9Condition specialBasis() { return special; }
            @Override public Art10Basis criminalBasis() { return criminal; }
        };
    }
}
