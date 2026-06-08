package com.iambilotta.gdpr.starter.logging;

import java.lang.reflect.Field;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import com.iambilotta.gdpr.annotations.GdprPersonalData;

/**
 * Renders an object for logging with every {@link GdprPersonalData}-annotated field masked,
 * while leaving non-personal fields in clear text (Art. 5(1)(f) integrity and confidentiality).
 *
 * <p>This is the reflection unit the {@code PiiMaskingConverter} delegates to. It deliberately
 * does a shallow pass over the declared fields of the object's own class hierarchy: the audit of
 * <em>deliberate</em> access lives in the AOP advisor; this only stops <em>accidental</em> PII in
 * the application log (someone logging a whole annotated object).
 *
 * <p>The annotation is {@code RUNTIME}-retained so the field-level classification is visible here.
 * Reflection results are cached per class because a log converter runs on the hot path.
 */
public class PiiMasker {

    /** Fixed replacement token. A fixed mask (not a partial reveal) is the safe default for logs. */
    public static final String MASK = "***";

    private final ConcurrentHashMap<Class<?>, Field[]> declaredFieldsCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Boolean> carriesPiiCache = new ConcurrentHashMap<>();

    /**
     * True if the value is a non-null object that declares at least one {@link GdprPersonalData}
     * field. Lets a caller skip the masking cost for ordinary log arguments (strings, numbers).
     */
    public boolean carriesPersonalData(Object value) {
        if (value == null) {
            return false;
        }
        Class<?> type = value.getClass();
        return carriesPiiCache.computeIfAbsent(type, this::scanForPii);
    }

    private boolean scanForPii(Class<?> type) {
        for (Field field : fieldsOf(type)) {
            if (field.isAnnotationPresent(GdprPersonalData.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Renders {@code value} as {@code SimpleName{field=value, pii=***, ...}} with personal-data
     * field values replaced by {@link #MASK}. A field whose value cannot be read is also masked
     * (fail-closed: never leak on a reflection error).
     */
    public String mask(Object value) {
        if (value == null) {
            return "null";
        }
        Class<?> type = value.getClass();
        StringJoiner body = new StringJoiner(", ", type.getSimpleName() + "{", "}");
        for (Field field : fieldsOf(type)) {
            if (field.isSynthetic()) {
                continue;
            }
            String rendered;
            if (field.isAnnotationPresent(GdprPersonalData.class)) {
                rendered = MASK;
            } else {
                rendered = readClearText(field, value);
            }
            body.add(field.getName() + "=" + rendered);
        }
        return body.toString();
    }

    private static String readClearText(Field field, Object owner) {
        try {
            field.setAccessible(true);
            Object fieldValue = field.get(owner);
            return String.valueOf(fieldValue);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // Fail closed: if we cannot read a field we do not know if it is PII, so mask it.
            return MASK;
        }
    }

    private Field[] fieldsOf(Class<?> type) {
        return declaredFieldsCache.computeIfAbsent(type, Class::getDeclaredFields);
    }
}
