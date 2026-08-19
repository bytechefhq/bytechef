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

package com.bytechef.runtime.job.configuration.workflow.contributor;

import com.bytechef.atlas.configuration.workflow.contributor.WorkflowReservedWordContributor;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import java.util.List;

/**
 * This app is deliberately assembled without {@code platform-configuration-service}, so it registers its own
 * {@link WorkflowReservedWordContributor} through {@code META-INF/services} rather than inheriting the shared one. The
 * <b>list</b> is not duplicated, though: it used to be a second set of local literals, and the two copies promptly
 * drifted, so both implementations now return {@link WorkflowExtConstants#RESERVED_WORDS} verbatim.
 * {@code platform-configuration-api} was already on this app's runtime classpath — depending on it explicitly makes it
 * visible at compile time and adds nothing to the scanned context.
 *
 * @author Ivica Cardic
 */
public class WorkflowReservedWordContributorImpl implements WorkflowReservedWordContributor {

    @Override
    public List<String> getReservedWords() {
        return WorkflowExtConstants.RESERVED_WORDS;
    }
}
