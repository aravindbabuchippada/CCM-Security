package com.ccm.security.listener;

import com.netflix.discovery.DiscoveryManager;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

/**
 * Manages the explicit deregistration and shutdown of the Netflix Eureka Discovery client.
 * This class ensures that when the Spring context is closed (e.g., during GlassFish undeployment) then
 * the application sends a "Cancel" signal to the Eureka server to remove itself from the service registry,
 * shuts down background heartbeat, cache-refresh threads managed by DiscoveryManager,
 * releases the ClassLoader references held by the Eureka client library.
 *
 * Without this explicit shutdown, the Eureka background threads may remain active after the application has stopped,
 * preventing the JVM from releasing file locks on Eureka-related JAR files in the WEB-INF/lib directory.
 */
@Component
public class EurekaShutdown {

    @PreDestroy
    public void shutdown() {
        DiscoveryManager.getInstance().shutdownComponent();
    }
}

