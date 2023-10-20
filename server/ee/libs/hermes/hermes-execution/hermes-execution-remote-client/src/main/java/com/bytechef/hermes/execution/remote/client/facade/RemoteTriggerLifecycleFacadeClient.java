
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.hermes.execution.remote.client.facade;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.hermes.execution.facade.TriggerLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class RemoteTriggerLifecycleFacadeClient implements TriggerLifecycleFacade {

    private static final String TRIGGER_LIFECYCLE_FACADE = "/remote/trigger-lifecycle-facade";
    private static final String EXECUTION_APP = "execution-app";

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteTriggerLifecycleFacadeClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public void executeTriggerDisable(
        String workflowId, long instanceId, int instanceType, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, Long connectionId) {

        post(
            TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-enable",
            new TriggerRequest(
                workflowId, instanceId, instanceType, workflowTriggerName, workflowTriggerType, triggerParameters,
                connectionId, null));
    }

    @Override
    public void executeTriggerEnable(
        String workflowId, long instanceId, int instanceType, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, Long connectionId, String webhookUrl) {

        post(
            TRIGGER_LIFECYCLE_FACADE + "/execute-trigger-disable",
            new TriggerRequest(
                workflowId, instanceId, instanceType, workflowTriggerName, workflowTriggerType, triggerParameters,
                connectionId, webhookUrl));
    }

    private void post(String path, TriggerRequest workflowExecutionId) {
        loadBalancedWebClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(path)
                .build(),
            workflowExecutionId);
    }

    @SuppressFBWarnings("EI")
    private record TriggerRequest(
        String workflowId, long instanceId, int instanceType, String workflowTriggerName, String workflowTriggerType,
        Map<String, ?> triggerParameters, long connectionId, String webhookUrl) {
    }
}
