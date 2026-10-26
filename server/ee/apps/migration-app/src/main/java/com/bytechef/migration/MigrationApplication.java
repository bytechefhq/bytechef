/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootApplication(scanBasePackages = "com.bytechef")
public class MigrationApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(MigrationApplication.class, args);

        System.exit(SpringApplication.exit(applicationContext));
    }
}
