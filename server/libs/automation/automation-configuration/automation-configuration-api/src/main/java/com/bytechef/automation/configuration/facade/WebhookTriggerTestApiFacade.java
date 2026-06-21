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

package com.bytechef.automation.configuration.facade;

/**
 * Per-controller facade for the automation webhook-trigger-test REST endpoints. It enforces workflow-scope
 * authorization and delegates to the shared platform {@code WebhookTriggerTestFacade} with
 * {@code PlatformType.AUTOMATION}.
 *
 * <p>
 * The shared facade is also reached by the embedded {@code /internal} controller (an API-key principal that carries no
 * workflow scope) and by the runtime webhook-test path (no user context), so the authorization cannot live on the
 * shared facade; it lives here, on the automation editor surface that always carries a platform user.
 *
 * @author Ivica Cardic
 */
public interface WebhookTriggerTestApiFacade {

    String enableTrigger(String workflowId, long environmentId);

    void disableTrigger(String workflowId, long environmentId);
}
