package com.iambilotta.gdpr.starter.logging;

import org.slf4j.helpers.MessageFormatter;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback pattern converter that renders the log message with every {@code @GdprPersonalData}
 * argument masked (Art. 5(1)(f)). Register it in a logback pattern as {@code %piimsg} and use it
 * in place of {@code %msg} / {@code %message}:
 *
 * <pre>{@code
 * <conversionRule conversionWord="piimsg"
 *     converterClass="com.iambilotta.gdpr.starter.logging.PiiMaskingConverter"/>
 * <pattern>%d %-5level %logger - %piimsg%n</pattern>
 * }</pre>
 *
 * <p>It re-runs the SLF4J {@code {}} substitution itself so it can mask each argument <em>before</em>
 * it lands in the formatted message: by the time {@code event.getFormattedMessage()} exists the PII
 * has already been stringified, so masking has to happen at the argument boundary. Arguments that
 * carry no personal data are left untouched (their normal {@code toString()} is used).
 */
public class PiiMaskingConverter extends ClassicConverter {

    // Stateless, thread-safe; one instance per pattern is fine.
    private final PiiMasker masker = new PiiMasker();

    @Override
    public String convert(ILoggingEvent event) {
        Object[] args = event.getArgumentArray();
        if (args == null || args.length == 0) {
            // No arguments: the message is a constant, nothing to mask.
            return event.getFormattedMessage();
        }
        Object[] maskedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            maskedArgs[i] = masker.carriesPersonalData(arg) ? masker.mask(arg) : arg;
        }
        return MessageFormatter.arrayFormat(event.getMessage(), maskedArgs).getMessage();
    }
}
