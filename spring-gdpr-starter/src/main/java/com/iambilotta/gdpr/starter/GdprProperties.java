package com.iambilotta.gdpr.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.gdpr")
public class GdprProperties {

    /**
     * Master switch. When false, no advisor, scheduler, or REST endpoint is registered.
     */
    private boolean enabled = true;

    private final Audit audit = new Audit();
    private final Retention retention = new Retention();
    private final Erasure erasure = new Erasure();
    private final Web web = new Web();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Audit getAudit() {
        return audit;
    }

    public Retention getRetention() {
        return retention;
    }

    public Erasure getErasure() {
        return erasure;
    }

    public Web getWeb() {
        return web;
    }

    public static class Audit {
        /**
         * Persist audit records to a database table. When false, audit goes through SLF4J only.
         */
        private boolean jdbcEnabled = false;

        /**
         * Database table for audit log. The schema must exist before the JDBC sink starts;
         * apply the migration shipped at {@code db/migration/V1__gdpr_audit_access.sql}
         * (Flyway) or {@code db/changelog/spring-gdpr-changelog.xml} (Liquibase) via
         * your existing migration tool.
         */
        private String table = "gdpr_audit_access";

        /**
         * Dev-only shortcut. When true, the JDBC sink issues
         * {@code CREATE TABLE IF NOT EXISTS} on first use. Default false: in production,
         * use Flyway / Liquibase / your tool of choice.
         */
        private boolean autoCreateSchema = false;

        private final Async async = new Async();

        public boolean isJdbcEnabled() {
            return jdbcEnabled;
        }

        public void setJdbcEnabled(boolean jdbcEnabled) {
            this.jdbcEnabled = jdbcEnabled;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public boolean isAutoCreateSchema() {
            return autoCreateSchema;
        }

        public void setAutoCreateSchema(boolean autoCreateSchema) {
            this.autoCreateSchema = autoCreateSchema;
        }

        public Async getAsync() {
            return async;
        }
    }

    public static class Async {
        /**
         * Wrap the audit sink with {@code AsyncAuditSinkDecorator}. Default true: production
         * deployments should never block the request thread on audit I/O. Set false only
         * for tests that need deterministic ordering, or for stacks that already wrap the
         * sink with their own dispatcher.
         */
        private boolean enabled = true;

        /**
         * Worker thread count. One is enough for log-aggregation and indexed JDBC writes.
         * Increase only if profiling shows the worker as the bottleneck.
         */
        private int threadCount = 1;

        /**
         * Bounded queue capacity. Drop-newest fallback when full: under saturation we keep
         * what is already in flight and the saturation surfaces via {@code dropped_total}
         * in WARN logs.
         */
        private int queueCapacity = 1024;

        /**
         * Milliseconds to wait for the executor to drain on shutdown before forcing termination.
         */
        private long awaitMillis = 5000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public long getAwaitMillis() {
            return awaitMillis;
        }

        public void setAwaitMillis(long awaitMillis) {
            this.awaitMillis = awaitMillis;
        }
    }

    public static class Retention {
        /**
         * Whether the retention scheduler runs. Disable to defer enforcement to a batch job.
         */
        private boolean enabled = true;

        /**
         * Cron expression for the retention sweep. Default: every day at 03:00.
         */
        private String cron = "0 0 3 * * *";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class Erasure {
        /**
         * Whether the {@code /gdpr/erasure} REST endpoint is exposed.
         */
        private boolean restEnabled = true;

        public boolean isRestEnabled() {
            return restEnabled;
        }

        public void setRestEnabled(boolean restEnabled) {
            this.restEnabled = restEnabled;
        }
    }

    public static class Web {
        /**
         * Base path for spring-gdpr REST endpoints. Default {@code /gdpr}.
         */
        private String basePath = "/gdpr";

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }
}
