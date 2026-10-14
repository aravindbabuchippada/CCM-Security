package com.ccm.security.listener;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * Ensures the graceful shutdown of all managed DataSources within the Spring Context.
 * This will intercept the application's shutdown signal and explicitly closes HikariCP connection pools.
 * This Helps prevent "ClassLoader leaks" in Jakarta EE containers like GlassFish.
 *
 * Without this explicit closure, background threads may continue to run after undeployment,
 * causing the container to maintain file locks on the underlying JDBC driver JAR files.
 */
@Component
public class DatasourceShutdown {

    private final List<DataSource> dataSources;

    public DatasourceShutdown(List<DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    @PreDestroy
    public void close() {
        for (DataSource ds : dataSources) {
            if (ds instanceof HikariDataSource hikari) {
                hikari.close();
            }
        }
    }
}
