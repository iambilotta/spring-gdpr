package com.iambilotta.gdpr.starter.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.iambilotta.gdpr.annotations.GdprPersonalData;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.read.ListAppender;

/**
 * Specifies REQ-GDPR-018 end-to-end at the log-output boundary: when an object carrying
 * {@code @GdprPersonalData} fields is logged through a pattern wired with the {@code %piimsg}
 * converter, the rendered log line must not contain the PII in clear text.
 */
class PiiMaskingConverterTest {

    static final class Account {
        @GdprPersonalData(category = GdprPersonalData.Category.CONTACT)
        String email = "victim@example.com";

        String accountId = "acct-7";
    }

    /**
     * @spec.given a Logback logger whose pattern uses the registered %piimsg converter
     * @spec.when  an object carrying a @GdprPersonalData field is logged as a message argument
     * @spec.then  the captured log line masks the PII value but keeps the non-personal field
     * @spec.us    REQ-GDPR-018
     */
    @Test
    @SuppressWarnings("unchecked")
    void rendersAMaskedMessageInTheLogOutput() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Register the %piimsg conversion rule the same way <conversionRule> does in XML:
        // a String->converterClass map in the context object store under PATTERN_RULE_REGISTRY.
        Map<String, String> ruleRegistry =
                (Map<String, String>) context.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
        if (ruleRegistry == null) {
            ruleRegistry = new HashMap<>();
            context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, ruleRegistry);
        }
        ruleRegistry.put("piimsg", PiiMaskingConverter.class.getName());

        PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern("%piimsg");
        layout.start();

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();

        Logger logger = context.getLogger(PiiMaskingConverterTest.class);
        logger.addAppender(appender);

        logger.info("loaded {}", new Account());

        ILoggingEvent event = appender.list.get(0);
        String rendered = layout.doLayout(event);

        assertThat(rendered).doesNotContain("victim@example.com");
        assertThat(rendered).contains("acct-7");
        assertThat(rendered).contains(PiiMasker.MASK);
    }
}
