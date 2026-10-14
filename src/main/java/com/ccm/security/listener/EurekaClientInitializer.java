package com.ccm.security.listener;

import com.ccm.security.config.CustomEurekaInstanceConfig;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/* EurekaClientInitializer -> Manages the Eureka client for a microservice, ensuring automatic registration
 *                             and re-registration with the Eureka server.
 * SmartLifecycle -> Allows controlling start/stop behavior when Spring context starts/stops.
 *                   Useful for lifecycle-dependent tasks like initializing Eureka clients.
 * */
@Component
public class EurekaClientInitializer implements SmartLifecycle {

    // Picked from the Active Spring Profile
    @Value("${EUREKA_URL}")
    private String eurekaUrl;

    // Dynamic scheduler interval comes from Profiles YAML
    @Value("${eureka.client.check-registration-interval-seconds}")
    private int checkIntervalSeconds;

    // Core objects for communicating with Eureka server and managing Instance status
    private EurekaClient eurekaClient;
    private ApplicationInfoManager appInfoManager;

    // Tracks SmartLifecycle state
    private boolean running = false;

    // Handles periodic registration checks.
    private ThreadPoolTaskScheduler scheduler;
    private ScheduledFuture<?> heartbeatChecker;

    // Prevents overlapping re-registration attempts.
    private final AtomicBoolean reRegistering = new AtomicBoolean(false);

    // Used to Initialize Eureka client registration
    private final CustomEurekaInstanceConfig instanceConfig;

    public EurekaClientInitializer(CustomEurekaInstanceConfig instanceConfig) {
        this.instanceConfig = instanceConfig;
    }

    private static final Logger logger = LoggerFactory.getLogger(EurekaClientInitializer.class);

    @Override
    public void start() {
        try {
            // Used to Initialize Eureka client registration
            appInfoManager = new ApplicationInfoManager(instanceConfig);

            DefaultEurekaClientConfig clientConfig = new DefaultEurekaClientConfig() {
                @Override
                public List<String> getEurekaServerServiceUrls(String myZone) {
                    return Collections.singletonList(eurekaUrl);
                }
            };
            eurekaClient = new DiscoveryClient(appInfoManager, clientConfig);

            // Marks this microservice as UP in Eureka after initial registration.
            appInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
            logger.info(
                    "Eureka Client registered successfully. InstanceId={}",
                    appInfoManager.getInfo().getInstanceId()
            );

            // Scheduler to ensure automatic re-registration if Eureka server restarts or heartbeats fail.
            scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.setThreadNamePrefix("eureka-heartbeat-");
            scheduler.initialize();

            running = true;

            heartbeatChecker = scheduler.scheduleAtFixedRate(
                    this::checkAndRegister,
                    TimeUnit.SECONDS.toMillis(checkIntervalSeconds)
            );
        } catch (Exception e) {
            logger.error("Error initializing Eureka Client: ", e);
        }
    }

    // Gracefully stop the scheduler and Eureka client during Spring shutdown.
    @Override
    public void stop() {
        logger.info("Stopping Eureka Client...");
        running = false;

        if (heartbeatChecker != null) {
            heartbeatChecker.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdown();

            try {
                scheduler.getScheduledExecutor().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
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
            eurekaClient.shutdown();
        }
        logger.info("Eureka Client stopped successfully.");
    }

    // Required by SmartLifecycle to track whether the component is active.
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Periodically checks if this instance is registered.
     * If not, forces re-registration.
     */
    private void checkAndRegister() {
        // Avoid multiple overlapping re-registration attempts.
        if (!reRegistering.compareAndSet(false, true)) {
            return;
        }

        try {
            // Checks if the microservice instance is still registered in Eureka.
            Application app = eurekaClient.getApplication(appInfoManager.getInfo().getAppName());
            boolean registered = app != null && app.getInstances() != null
                    && app.getInstances().stream()
                    .anyMatch(i -> i.getHostName().equals(appInfoManager.getInfo().getHostName()) && i.getPort() == appInfoManager.getInfo().getPort());

            if (!registered) {
                logger.warn("Eureka instance missing. Recreating DiscoveryClient for re-registration...");

                /* If missing, shut down the old client.
                 *  Re-create a new DiscoveryClient pointing to Eureka.
                 *  Force status UP → ensures the microservice is immediately visible in Eureka.
                 * */
                if (eurekaClient != null) eurekaClient.shutdown();
                DefaultEurekaClientConfig clientConfig = new DefaultEurekaClientConfig() {
                    @Override
                    public List<String> getEurekaServerServiceUrls(String myZone) {
                        return Collections.singletonList(eurekaUrl);
                    }
                };
                TimeUnit.SECONDS.sleep(2);
                eurekaClient = new DiscoveryClient(appInfoManager, clientConfig);
                appInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);

                logger.info("Re-registered with Eureka successfully.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        catch (Exception ex) {
            logger.warn("Unable to communicate with Eureka Server. Registration will be retried.", ex);
        } finally {
            // Release the re-registration lock so the next scheduler cycle can run.
            reRegistering.set(false);
        }
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    public EurekaClient getEurekaClient() {
        return eurekaClient;
    }
}