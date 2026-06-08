package com.iambilotta.gdpr.starter.access;

import com.iambilotta.gdpr.annotations.GdprPersonalData;

/**
 * One classified field in a subject access export (Art. 15): which source type held it, the field
 * name, its current value rendered as text, the declared description and category.
 */
public record ExportedField(
        String sourceType,
        String field,
        String value,
        String description,
        GdprPersonalData.Category category) {
}
