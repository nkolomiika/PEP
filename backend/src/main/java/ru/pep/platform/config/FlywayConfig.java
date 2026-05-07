package ru.pep.platform.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In dev/demo we frequently rewrite migration files (esp. user-stand schema). Stock Flyway
 * then refuses to start with a checksum mismatch. This strategy repairs the schema_history
 * (rewrites stored checksums to match current files; no SQL is re-executed) before running
 * the normal migrate step, so devs don't need to manually drop the postgres volume.
 *
 * <p>repair() is idempotent — when checksums already match it does nothing.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairAndMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
