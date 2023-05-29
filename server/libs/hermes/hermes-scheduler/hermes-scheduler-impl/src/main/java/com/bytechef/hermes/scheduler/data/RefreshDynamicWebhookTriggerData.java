
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.scheduler.data;

import com.bytechef.hermes.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;

/**
 * @author Ivica Cardic
 */
public class RefreshDynamicWebhookTriggerData {

    private final Data data;

    @SuppressFBWarnings("EI")
    public RefreshDynamicWebhookTriggerData(
        WorkflowExecutionId workflowExecutionId, String componentName, int componentVersion) {

        this.data = new Data(workflowExecutionId, componentName, componentVersion);
    }

    public Data getData() {
        return data;
    }

    @Override
    public String toString() {
        return "TriggerDynamicWebhookRefreshData{" +
            "data=" + data +
            ", schedule=" + null +
            '}';
    }

    @SuppressFBWarnings("EI")
    public record Data(WorkflowExecutionId workflowExecutionId, String componentName, int componentVersion)
        implements Serializable {
    }
}
