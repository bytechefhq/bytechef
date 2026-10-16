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
public final class ProjectOwnerContributor {

    private final ProjectOwner projectOwner;
    private final Supplier<OwnerConditionTemplate> ownerConditionTemplateSupplier;

    private ProjectOwnerContributor(
        ProjectOwner projectOwner, Supplier<OwnerConditionTemplate> ownerConditionTemplateSupplier) {

        this.projectOwner = projectOwner;
        this.ownerConditionTemplateSupplier = ownerConditionTemplateSupplier;
    }

    public static ProjectOwnerContributor of(ProjectOwner projectOwner, String conditionTemplate) {
        OwnerConditionTemplate ownerConditionTemplate = new OwnerConditionTemplate(conditionTemplate);

        return new ProjectOwnerContributor(projectOwner, () -> ownerConditionTemplate);
    }

    public static ProjectOwnerContributor of(
        ProjectOwner projectOwner, Supplier<String> conditionTemplateSupplier) {

        return new ProjectOwnerContributor(
            projectOwner, () -> new OwnerConditionTemplate(conditionTemplateSupplier.get()));
    }

    public ProjectOwner getProjectOwner() {
        return projectOwner;
    }

    public String getProjectClaimCondition(String projectIdColumn) {
        OwnerConditionTemplate ownerConditionTemplate = ownerConditionTemplateSupplier.get();

        return ownerConditionTemplate.format(projectIdColumn);
    }
}
