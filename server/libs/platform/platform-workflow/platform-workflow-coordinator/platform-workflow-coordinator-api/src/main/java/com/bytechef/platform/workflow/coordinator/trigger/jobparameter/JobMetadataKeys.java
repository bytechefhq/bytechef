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

package com.bytechef.platform.workflow.coordinator.trigger.jobparameter;

/**
 * Reserved keys the platform coordinator stores inside an Atlas {@code Job.metadata} map.
 *
 * @author Ivica Cardic
 */
public final class JobMetadataKeys {

    /**
     * Map-valued entry carrying the Spring Batch {@code JobParameter} overrides for the spawned job.
     */
    public static final String JOB_PARAMETERS = "__jobParameters";

    private JobMetadataKeys() {
    }
}
