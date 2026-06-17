package com.iambilotta.gdpr.starter.autoconfig;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import com.iambilotta.gdpr.annotations.GdprErasable;
import com.iambilotta.gdpr.starter.audit.ActorResolver;
import com.iambilotta.gdpr.starter.audit.AuditSink;
import com.iambilotta.gdpr.starter.erasure.crypto.CryptoShreddingErasureHandler;
import com.iambilotta.gdpr.starter.erasure.crypto.SubjectKeyStore;
import com.iambilotta.gdpr.starter.erasure.forgettable.ForgettablePayloadErasureHandler;
import com.iambilotta.gdpr.starter.erasure.forgettable.ForgettablePayloadStore;

/**
 * Closes the declarative-erasure DX gap (issue #36): scans the application packages for types
 * annotated {@link GdprErasable} whose {@link GdprErasable.Strategy strategy} is
 * {@link GdprErasable.Strategy#CRYPTO_SHRED} or {@link GdprErasable.Strategy#FORGETTABLE}, and
 * registers the matching {@code ErasureHandler} bean for each, so declaring the annotation routes the
 * type's right-to-erasure through the existing crypto / forgettable machinery (ADR-0009 / ADR-0010)
 * with <strong>zero hand-wiring</strong>. The store / key-store beans are contributed (and
 * overridable) by {@link GdprAutoConfiguration}.
 *
 * <p>This is the same scan-and-register pattern Spring Data uses for repositories: an
 * {@link ImportBeanDefinitionRegistrar} that reads the auto-configuration base packages and adds one
 * bean definition per discovered type. The handler's collaborators (the store / key store, the
 * {@code AuditSink}, the {@code ActorResolver}) are wired by type at instantiation, so the scan only
 * needs the entity {@link Class} and the declared {@link GdprErasable#order()}.
 *
 * <p><strong>DELETE / ANONYMIZE / PSEUDONYMIZE are untouched.</strong> They stay the adopter's own
 * {@code ErasureHandler} (ADR-0004): the library cannot know how to delete an arbitrary store. Only
 * the two append-only-safe strategies have a reusable library handler to auto-wire.
 */
public class GdprErasableScannerRegistrar
        implements ImportBeanDefinitionRegistrar, BeanFactoryAware, ResourceLoaderAware, EnvironmentAware {

    private static final Logger LOG = LoggerFactory.getLogger(GdprErasableScannerRegistrar.class);

    private BeanFactory beanFactory;
    private ResourceLoader resourceLoader;
    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!AutoConfigurationPackages.has(beanFactory)) {
            LOG.debug("No auto-configuration base packages available; skipping @GdprErasable scan.");
            return;
        }
        List<String> basePackages = AutoConfigurationPackages.get(beanFactory);
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, environment);
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(GdprErasable.class));

        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                registerHandlerFor(candidate.getBeanClassName(), registry);
            }
        }
    }

    private void registerHandlerFor(String className, BeanDefinitionRegistry registry) {
        Class<?> entityType;
        try {
            entityType = Class.forName(className, false, resourceLoader.getClassLoader());
        } catch (ClassNotFoundException ex) {
            LOG.warn("Could not load @GdprErasable type {} for auto-wiring; skipping.", className, ex);
            return;
        }
        GdprErasable erasable = entityType.getAnnotation(GdprErasable.class);
        if (erasable == null) {
            return;
        }
        switch (erasable.strategy()) {
            case CRYPTO_SHRED -> register(
                    registry, entityType, erasable.order(), CryptoShreddingErasureHandler.class);
            case FORGETTABLE -> register(
                    registry, entityType, erasable.order(), ForgettablePayloadErasureHandler.class);
            default -> { /* DELETE / ANONYMIZE / PSEUDONYMIZE stay the adopter's own handler (ADR-0004) */ }
        }
    }

    /**
     * Registers one handler bean for the entity type. The store / key store, the audit sink and the
     * actor resolver are resolved by <strong>type</strong> (so an adopter who overrides any of them
     * under a different bean name is still picked up); the entity {@link Class} and the
     * {@link GdprErasable#order()} are constructor values. Skips if a handler bean for this type was
     * already registered (idempotent across repeated imports).
     */
    private void register(
            BeanDefinitionRegistry registry, Class<?> entityType, int order, Class<?> handlerType) {
        String beanName = "gdprErasureHandler#" + entityType.getName();
        if (registry.containsBeanDefinition(beanName)) {
            return;
        }
        Class<?> storeType = handlerType == CryptoShreddingErasureHandler.class
                ? SubjectKeyStore.class
                : ForgettablePayloadStore.class;
        AbstractBeanDefinition definition = BeanDefinitionBuilder
                .genericBeanDefinition(handlerType)
                .addConstructorArgValue(new RuntimeBeanReference(storeType))
                .addConstructorArgValue(new RuntimeBeanReference(AuditSink.class))
                .addConstructorArgValue(new RuntimeBeanReference(ActorResolver.class))
                .addConstructorArgValue(entityType)
                .addConstructorArgValue(order)
                .getBeanDefinition();
        registry.registerBeanDefinition(beanName, definition);
        LOG.debug("Auto-wired {} for @GdprErasable type {} (order={}).",
                handlerType.getSimpleName(), entityType.getName(), order);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
