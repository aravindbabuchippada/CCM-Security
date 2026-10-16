package com.ccm.security.listener;

import com.ccm.security.config.CustomEurekaInstanceConfig;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/* EurekaClientInitializer -> Manages the Eureka client for a microservice, ensuring automatic registration
 *                             and re-registration with the Eureka server.
 * SmartLifecycle -> Allows controlling start/stop behavior when Spring context starts/stops.
 *                   Useful for lifecycle-dependent tasks like initializing Eureka clients.
 * */
@Component
public class EurekaClientInitializer implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(EurekaClientInitializer.class);

    @Value("${EUREKA_URL}")
    private String eurekaUrl;

    @Value("${eureka.client.registry-fetch-interval-seconds}")
    private int fetchInterval;

    @Value("${eureka.client.initial-instance-info-replication-interval-seconds}")
    private int replicationInterval;

    @Value("${eureka.client.reregister-interval-seconds}")
    private int reregisterInterval;

    private final CustomEurekaInstanceConfig instanceConfig;
    private ApplicationInfoManager appInfoManager;
    private EurekaClient eurekaClient;
    private volatile boolean running = false;
    private ThreadPoolTaskScheduler scheduler;

    public EurekaClientInitializer(CustomEurekaInstanceConfig instanceConfig) {
        this.instanceConfig = instanceConfig;
    }

    @Override
    public void start() {
        try {
            appInfoManager = new ApplicationInfoManager(instanceConfig);

            DefaultEurekaClientConfig clientConfig = new DefaultEurekaClientConfig() {
                @Override
                public List<String> getEurekaServerServiceUrls(String myZone) {
                    return Collections.singletonList(eurekaUrl);
                }

                @Override
                public int getRegistryFetchIntervalSeconds() {
                    return fetchInterval;
                }

                @Override
                public int getInitialInstanceInfoReplicationIntervalSeconds() {
                    return replicationInterval;
                }
            };
            eurekaClient = new DiscoveryClient(appInfoManager, clientConfig);
            appInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
            running = true;

            scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.setThreadNamePrefix("eureka-reregister-");
            scheduler.initialize();
            scheduler.scheduleWithFixedDelay(this::markDirty, Duration.ofSeconds(reregisterInterval));

            logger.info("Eureka Client registered. InstanceId={}", appInfoManager.getInfo().getInstanceId());
        } catch (Exception e) {
            logger.error("Error initializing Eureka Client", e);
        }
    }

    private void markDirty() {
        try {
            appInfoManager.registerAppMetadata(
                    Map.of("lastRefresh", String.valueOf(System.currentTimeMillis())));
        } catch (Exception e) {
            logger.warn("Unable to trigger Eureka re-registration", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Stopping Eureka Client...");
        running = false;

        if (scheduler != null) {
            scheduler.shutdown();
        }
        try {
            if (appInfoManager != null) {
                appInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            logger.warn("Unable to mark instance DOWN", ex);
        }
        if (eurekaClient != null) {
            eurekaClient.shutdown();   // also deregisters
        }
        logger.info("Eureka Client stopped.");
    }

    @Override public boolean isRunning() { return running; }
    @Override public int getPhase() { return Integer.MAX_VALUE; }
    @Override public boolean isAutoStartup() { return true; }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }
}