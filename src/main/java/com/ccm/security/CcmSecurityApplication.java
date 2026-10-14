package com.ccm.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = {"com.ccm.security", "com.ccm.common"})
@ComponentScan(basePackages = { "com.ccm.*"})
@EntityScan(basePackages = {"com.ccm.security.entity", "com.ccm.common.entity"})
public class CcmSecurityApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(CcmSecurityApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(CcmSecurityApplication.class, args);
	}

}
