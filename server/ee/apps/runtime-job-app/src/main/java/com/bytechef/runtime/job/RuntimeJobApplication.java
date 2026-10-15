/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.runtime.job.executor.JobRunner;
import com.bytechef.runtime.job.listener.JobStatusApplicationEventListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */

@SpringBootApplication(scanBasePackages = "com.bytechef")
public class RuntimeJobApplication implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RuntimeJobApplication.class);

    private final JobRunner jobRunner;
    private final JobStatusApplicationEventListener jobStatusApplicationEventListener;

    @SuppressFBWarnings("EI")
    public RuntimeJobApplication(
        JobRunner jobRunner, JobStatusApplicationEventListener jobStatusApplicationEventListener) {

        this.jobRunner = jobRunner;
        this.jobStatusApplicationEventListener = jobStatusApplicationEventListener;
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(RuntimeJobApplication.class, args);

        System.exit(SpringApplication.exit(context));
    }

    @Override
    public void run(ApplicationArguments args) throws InterruptedException, IOException {
        String workflow = getOptionValue(args, "workflow");

        if (workflow == null) {
            throw new IllegalArgumentException("Workflow name is required");
        }

        String parameters = getOptionValue(args, "parameters");
        String connections = getOptionValue(args, "connections");
        String timeout = getOptionValue(args, "timeout");
        String outputsFile = getOptionValue(args, "outputs-file");

        Duration timeoutDuration = timeout == null ? null : DurationStyle.detectAndParse(timeout);
        Map<String, ?> jobParameters = parameters == null ? Map.of() : JsonUtils.readMap(parameters);

        log.info(
            "Running workflow: {} with parameters: {} and connections: {}",
            workflow, parameters == null ? "{}" : parameters, connections == null ? "{}" : connections);

        long jobId = jobRunner.run(workflow, jobParameters);

        Job.Status status = jobStatusApplicationEventListener.awaitTermination(jobId, timeoutDuration);

        if (status == Job.Status.COMPLETED) {
            writeOutputs(jobId, outputsFile);
        }
    }

    private void writeOutputs(long jobId, @Nullable String outputsFile) throws IOException {
        String outputs = JsonUtils.write(jobRunner.getOutputs(jobId));

        log.info("Job id={} outputs: {}", jobId, outputs);

        if (outputsFile != null) {
            Files.writeString(Path.of(outputsFile), outputs);
        }
    }

    @Nullable
    private static String getOptionValue(ApplicationArguments args, String name) {
        List<String> optionValues = args.getOptionValues(name);

        if (optionValues == null || optionValues.isEmpty()) {
            return null;
        }

        return optionValues.getFirst();
    }
}
