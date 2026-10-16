package com.ccm.security.config;

import com.netflix.appinfo.MyDataCenterInstanceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CustomEurekaInstanceConfig extends MyDataCenterInstanceConfig {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${eureka.instance.hostname}")
    private String hostName;

    @Value("${eureka.instance.non-secure-port}")
    private int port;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Value("${eureka.instance.lease-renewal-interval-in-seconds}")
    private int leaseRenewalInterval;

    @Value("${eureka.instance.lease-expiration-duration-in-seconds}")
    private int leaseExpirationDuration;

    @Override
    public String getAppname() {
        return appName;
    }

    @Override
    public String getVirtualHostName() {
        return appName;
    }

    @Override
    public String getSecureVirtualHostName() {
        return appName;
    }

    @Override
    public String getHostName(boolean refresh) {
        return hostName;
    }

    @Override
    public int getNonSecurePort() {
        return port;
    }

    @Override
    public boolean isNonSecurePortEnabled() {
        return true;
    }

    @Override
    public String getInstanceId() {
        return appName + ":" + hostName + ":" + port;
    }

    @Override
    public String getHomePageUrl() {
        return "http://" + hostName + ":" + port + contextPath;
    }

    @Override
    public String getStatusPageUrl() {
        return getHomePageUrl() + "/actuator/info";
    }

    @Override
    public String getHealthCheckUrl() {
        return getHomePageUrl() + "/actuator/health";
    }

    @Override
    public int getLeaseRenewalIntervalInSeconds() {
        return leaseRenewalInterval;
    }

    @Override
    public int getLeaseExpirationDurationInSeconds() {
        return leaseExpirationDuration;
    }
}