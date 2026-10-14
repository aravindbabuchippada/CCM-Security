package com.ccm.security.listener;

import jakarta.annotation.PreDestroy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Ensures the graceful closure of Redis connections and resources during application shutdown.
 * This class is responsible for manually releasing the RedisConnectionFactory resources
 * when the Spring ApplicationContext is destroyed.
 *
 * Its primary objectives are:
 * 1. Closing active sockets and network buffers connected to the Redis server.
 * 2. Terminating background worker threads and connection pools (e.g., Lettuce/Jedis pools).
 * 3. Ensuring the ClassLoader can be fully collected by the JVM.
 * 4. In containerized environments like GlassFish on Windows, failing to explicitly close the connection factory
 *    can leave network threads active. These threads keep the application's JAR files locked, causing undeployment failures.
 */
@Component
public class RedisShutdown {

    private final RedisConnectionFactory factory;

    public RedisShutdown(RedisConnectionFactory factory) {
        this.factory = factory;
    }

    @PreDestroy
    public void shutdown() {
        factory.getConnection().close();
    }
}
