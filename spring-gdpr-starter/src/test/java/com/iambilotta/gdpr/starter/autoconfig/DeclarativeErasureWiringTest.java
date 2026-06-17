package com.iambilotta.gdpr.starter.autoconfig;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.iambilotta.gdpr.starter.autoconfig.declfixture.crypto.CryptoShreddedEntity;
import com.iambilotta.gdpr.starter.autoconfig.declfixture.forgettable.ForgettableEntity;
import com.iambilotta.gdpr.starter.erasure.ErasureService;
import com.iambilotta.gdpr.starter.erasure.crypto.CryptoShreddingErasureHandler;
import com.iambilotta.gdpr.starter.erasure.crypto.InMemorySubjectKeyStore;
import com.iambilotta.gdpr.starter.erasure.crypto.SubjectKeyStore;
import com.iambilotta.gdpr.starter.erasure.forgettable.ForgettablePayloadErasureHandler;
import com.iambilotta.gdpr.starter.erasure.forgettable.ForgettablePayloadStore;
import com.iambilotta.gdpr.starter.erasure.forgettable.InMemoryForgettablePayloadStore;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Specification for declarative append-only-safe erasure (issue #36): declaring
 * {@code @GdprErasable(strategy = CRYPTO_SHRED | FORGETTABLE)} on a type, with NO manual bean wiring,
 * routes that type's right-to-erasure through the existing crypto / forgettable handler (ADR-0009 /
 * ADR-0010). The store / key-store beans are contributed by the starter and overridable.
 *
 * <p>The three fixture types each live in their own package (under {@code declfixture/}) so the
 * classpath scan rooted at one package finds exactly one type, keeping each assertion isolated.
 */
class DeclarativeErasureWiringTest {

    private static final String CRYPTO_PKG = "com.iambilotta.gdpr.starter.autoconfig.declfixture.crypto";
    private static final String FORGETTABLE_PKG = "com.iambilotta.gdpr.starter.autoconfig.declfixture.forgettable";
    private static final String LEGACY_PKG = "com.iambilotta.gdpr.starter.autoconfig.declfixture.legacy";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GdprAutoConfiguration.class))
            .withPropertyValues("spring.gdpr.audit.async.enabled=false");

    /**
     * @spec.given a type annotated @GdprErasable(strategy = CRYPTO_SHRED) and no hand-wired handler
     * @spec.when  the context starts with that type's package as the scanned base
     * @spec.then  a CryptoShreddingErasureHandler for that type is auto-wired and visible to the
     *             ErasureService (issue #36, the declarative bridge to the ADR-0009 machinery)
     * @spec.adr   ADR-0009
     * @spec.us    REQ-GDPR-024
     */
    @Test
    void autoWiresACryptoShreddingHandlerForACryptoShredAnnotatedType() {
        runner.withUserConfiguration(CryptoScan.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(CryptoShreddingErasureHandler.class);
            CryptoShreddingErasureHandler handler = ctx.getBean(CryptoShreddingErasureHandler.class);
            assertThat(handler.entityType()).isEqualTo(CryptoShreddedEntity.class);
            assertThat(ctx.getBean(ErasureService.class).registeredTypes())
                    .contains(CryptoShreddedEntity.class.getName());
        });
    }

    /**
     * @spec.given a type annotated @GdprErasable(strategy = FORGETTABLE, order = 30)
     * @spec.when  the context starts with that type's package as the scanned base
     * @spec.then  a ForgettablePayloadErasureHandler is auto-wired with the declared order and is
     *             visible to the ErasureService (issue #36, the ADR-0010 primary path, declarative)
     * @spec.adr   ADR-0010
     * @spec.us    REQ-GDPR-024
     */
    @Test
    void autoWiresAForgettablePayloadHandlerForAForgettableAnnotatedTypeWithItsOrder() {
        runner.withUserConfiguration(ForgettableScan.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(ForgettablePayloadErasureHandler.class);
            ForgettablePayloadErasureHandler handler = ctx.getBean(ForgettablePayloadErasureHandler.class);
            assertThat(handler.entityType()).isEqualTo(ForgettableEntity.class);
            assertThat(handler.order()).isEqualTo(30);
            assertThat(ctx.getBean(ErasureService.class).registeredTypes())
                    .contains(ForgettableEntity.class.getName());
        });
    }

    /**
     * @spec.given a type annotated with the legacy strategy DELETE (the adopter owns the handler)
     * @spec.when  the context starts with that type's package as the scanned base
     * @spec.then  no library handler is auto-wired for it: DELETE/ANONYMIZE/PSEUDONYMIZE stay the
     *             adopter's ErasureHandler (ADR-0004), only the append-only-safe strategies are wired
     * @spec.adr   ADR-0004
     * @spec.us    REQ-GDPR-024
     */
    @Test
    void doesNotAutoWireAHandlerForTheLegacyDeleteStrategy() {
        runner.withUserConfiguration(LegacyScan.class).run(ctx -> {
            assertThat(ctx).doesNotHaveBean(CryptoShreddingErasureHandler.class);
            assertThat(ctx).doesNotHaveBean(ForgettablePayloadErasureHandler.class);
        });
    }

    /**
     * @spec.given declaring CRYPTO_SHRED with no DataSource on the context
     * @spec.when  the context starts
     * @spec.then  an in-memory SubjectKeyStore is contributed (dev/test fallback) and the wired
     *             handler runs end to end against it
     * @spec.us    REQ-GDPR-024
     */
    @Test
    void contributesAnInMemoryKeyStoreFallbackWhenNoDataSourceIsPresent() {
        runner.withUserConfiguration(CryptoScan.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(SubjectKeyStore.class);
            assertThat(ctx.getBean(SubjectKeyStore.class)).isInstanceOf(InMemorySubjectKeyStore.class);
            int affected = ctx.getBean(CryptoShreddingErasureHandler.class).erase("alice-1");
            assertThat(affected).isZero(); // no key minted yet, but the path is wired and runs
        });
    }

    /**
     * @spec.given an adopter that declares their own SubjectKeyStore bean
     * @spec.when  the context starts with a CRYPTO_SHRED type
     * @spec.then  the adopter's store wins (the default is @ConditionalOnMissingBean, overridable)
     * @spec.us    REQ-GDPR-024
     */
    @Test
    void theDefaultStoreBeansAreOverridable() {
        runner.withUserConfiguration(CryptoScan.class, CustomKeyStoreConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(SubjectKeyStore.class);
            assertThat(ctx.getBean(SubjectKeyStore.class)).isSameAs(CustomKeyStoreConfig.CUSTOM);
        });
    }

    /**
     * @spec.given a FORGETTABLE type with no DataSource
     * @spec.when  the context starts
     * @spec.then  an in-memory ForgettablePayloadStore is contributed as the dev/test fallback
     * @spec.us    REQ-GDPR-024
     */
    @Test
    void contributesAnInMemoryForgettableStoreFallbackWhenNoDataSourceIsPresent() {
        runner.withUserConfiguration(ForgettableScan.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(ForgettablePayloadStore.class);
            assertThat(ctx.getBean(ForgettablePayloadStore.class))
                    .isInstanceOf(InMemoryForgettablePayloadStore.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @AutoConfigurationPackage(basePackages = CRYPTO_PKG)
    static class CryptoScan {
    }

    @Configuration(proxyBeanMethods = false)
    @AutoConfigurationPackage(basePackages = FORGETTABLE_PKG)
    static class ForgettableScan {
    }

    @Configuration(proxyBeanMethods = false)
    @AutoConfigurationPackage(basePackages = LEGACY_PKG)
    static class LegacyScan {
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomKeyStoreConfig {
        static final SubjectKeyStore CUSTOM = new InMemorySubjectKeyStore();

        @Bean
        SubjectKeyStore myKmsKeyStore() {
            return CUSTOM;
        }

        @Bean
        DataSource dataSource() {
            org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
            ds.setURL("jdbc:h2:mem:declarative-override;DB_CLOSE_DELAY=-1");
            ds.setUser("sa");
            ds.setPassword("");
            return ds;
        }
    }
}
