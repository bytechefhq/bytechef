/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Triggers {@link AiModelCatalogReconciler} once the application is ready and daily thereafter.
 *
 * <p>
 * The startup run is {@link Async} on purpose: it means a fresh deployment that has configured a provider has a
 * populated model list without waiting a day or finding a button, while the multi-megabyte catalog parse and the
 * resulting writes stay off the startup thread.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI2")
public class AiModelCatalogReconcilerScheduler {

    private final AiModelCatalogReconciler reconciler;

    public AiModelCatalogReconcilerScheduler(AiModelCatalogReconciler reconciler) {
        this.reconciler = reconciler;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnStartup() {
        reconciler.reconcile();
    }

    @Scheduled(initialDelayString = "P1D", fixedDelayString = "P1D")
    public void reconcileOnSchedule() {
        reconciler.reconcile();
    }
}
