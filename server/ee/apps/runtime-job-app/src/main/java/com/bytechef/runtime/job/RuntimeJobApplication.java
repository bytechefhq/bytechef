/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.runtime.job.executor.JobRunner;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */

@SpringBootApplication(scanBasePackages = "com.bytechef")
public class RuntimeJobApplication implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RuntimeJobApplication.class);

    private final JobRunner jobRunner;

    public RuntimeJobApplication(JobRunner jobRunner) {
        this.jobRunner = jobRunner;
    }

    public static void main(String[] args) {
        SpringApplication.run(RuntimeJobApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> workflow = args.getOptionValues("workflow");

        if (workflow == null) {
            throw new IllegalArgumentException("Workflow name is required");
        }

        List<String> parameters = args.getOptionValues("parameters");
        List<String> connections = args.getOptionValues("connections");

        log.info(
            "Running workflow: {} with parameters: {} and connections: {}",
            workflow, parameters == null ? "{}" : parameters, connections == null ? "{}" : connections);

        Map<String, ?> jobParameters = parameters == null ? Map.of() : JsonUtils.readMap(parameters.getFirst());

        jobRunner.run(workflow.getFirst(), jobParameters);
    }
}
