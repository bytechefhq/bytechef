/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.ownership;

import java.util.function.Supplier;

/**
 * @author Ivica Cardic
 */
public final class ProjectDeploymentOwnerContributor {

    private final ProjectDeploymentOwner projectDeploymentOwner;
    private final Supplier<OwnerConditionTemplate> ownerConditionTemplateSupplier;

    private ProjectDeploymentOwnerContributor(
        ProjectDeploymentOwner projectDeploymentOwner,
        Supplier<OwnerConditionTemplate> ownerConditionTemplateSupplier) {

        this.projectDeploymentOwner = projectDeploymentOwner;
        this.ownerConditionTemplateSupplier = ownerConditionTemplateSupplier;
    }

    public static ProjectDeploymentOwnerContributor
        of(ProjectDeploymentOwner projectDeploymentOwner, String conditionTemplate) {
        OwnerConditionTemplate ownerConditionTemplate = new OwnerConditionTemplate(conditionTemplate);

        return new ProjectDeploymentOwnerContributor(projectDeploymentOwner, () -> ownerConditionTemplate);
    }

    public static ProjectDeploymentOwnerContributor of(
        ProjectDeploymentOwner projectDeploymentOwner, Supplier<String> conditionTemplateSupplier) {

        return new ProjectDeploymentOwnerContributor(
            projectDeploymentOwner, () -> new OwnerConditionTemplate(conditionTemplateSupplier.get()));
    }

    public ProjectDeploymentOwner getProjectDeploymentOwner() {
        return projectDeploymentOwner;
    }

    public String getProjectDeploymentClaimCondition(String projectDeploymentIdColumn) {
        OwnerConditionTemplate ownerConditionTemplate = ownerConditionTemplateSupplier.get();

        return ownerConditionTemplate.format(projectDeploymentIdColumn);
    }
}
