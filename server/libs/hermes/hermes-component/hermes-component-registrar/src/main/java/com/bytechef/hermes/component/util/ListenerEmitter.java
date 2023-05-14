package com.bytechef.hermes.component.util;

import com.bytechef.atlas.message.broker.TaskMessageRoute;
import com.bytechef.hermes.domain.TriggerExecution;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.bytechef.message.broker.MessageBroker;

/**
 * @author Ivica Cardic
 */
public class ListenerEmitter implements ListenerTriggerUtils.ListenerEmitter {

    private final MessageBroker messageBroker;

    public ListenerEmitter(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    @Override
    public void emit(String workflowExecutionId, Object output) {
        TriggerExecution triggerExecution = TriggerExecution.builder()
            .output(output)
            .workflowExecutionId(WorkflowExecutionId.parse(workflowExecutionId))
            .build();

        messageBroker.send(TaskMessageRoute.TASKS_COMPLETIONS, triggerExecution);
    }
}
