/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.dto;

import org.jspecify.annotations.Nullable;

/**
 * A project referenced by the source resource being promoted (through the workflows the resource exposes), together
 * with the version already present in the target environment, if any.
 *
 * @param projectId            the id of the project in the source environment
 * @param projectName          the project name, shown to the user reviewing the promotion
 * @param sourceProjectVersion the published project version in the source environment
 * @param targetProjectVersion the published project version already present in the target environment, or {@code null}
 *                             when the project does not exist there yet
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record PromotionProjectPreview(
    long projectId, String projectName, int sourceProjectVersion, @Nullable Integer targetProjectVersion) {
}
