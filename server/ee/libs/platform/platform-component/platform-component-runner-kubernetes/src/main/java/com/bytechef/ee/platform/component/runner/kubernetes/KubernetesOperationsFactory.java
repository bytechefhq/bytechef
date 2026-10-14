/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import java.time.Duration;

/**
 * @author Ivica Cardic
 * @version ee
 */
@FunctionalInterface
interface KubernetesOperationsFactory {

    KubernetesOperations create(KubernetesConnectionSettings connectionSettings, Duration requestTimeout);
}
