/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class JobStatusApplicationEventListener implements ApplicationEventListener {

    private final ConfigurableApplicationContext context;

    @SuppressFBWarnings("EI")
    public JobStatusApplicationEventListener(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent) {
            if (jobStatusApplicationEvent.getStatus() == Job.Status.COMPLETED) {
                SpringApplication.exit(context, () -> 0);
            } else if (jobStatusApplicationEvent.getStatus() == Job.Status.FAILED) {
                SpringApplication.exit(context, () -> -1);
            }
        }
    }
}
