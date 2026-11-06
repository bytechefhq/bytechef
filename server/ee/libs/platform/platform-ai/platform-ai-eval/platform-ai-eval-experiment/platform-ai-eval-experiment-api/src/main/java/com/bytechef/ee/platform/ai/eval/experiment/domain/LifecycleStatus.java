/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.domain;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface LifecycleStatus {

    String name();

    default boolean isTerminal() {
        String statusName = name();

        return "COMPLETED".equals(statusName) || "FAILED".equals(statusName);
    }
}
