/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime;

import com.bytechef.config.ApplicationProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableConfigurationProperties(ApplicationProperties.class)
@SpringBootApplication(scanBasePackages = "com.bytechef")
public class RuntimeApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RuntimeApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RuntimeApplication.class, args);
    }

    @Override
    @SuppressFBWarnings("DM_EXIT")
    public void run(String... args) {
        log.info("EXECUTING : command line runner");

        for (int i = 0; i < args.length; ++i) {
            log.info("args[{}]: {}", i, args[i]);
        }

        System.exit(0);
    }
}
