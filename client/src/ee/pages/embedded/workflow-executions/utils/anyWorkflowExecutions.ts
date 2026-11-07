import {
    EmbeddedWorkflowExecutionFromJSON,
    EmbeddedWorkflowExecutionSourceEnum,
    WorkflowExecution as EmbeddedWorkflowExecution,
} from '@/ee/shared/middleware/embedded/workflow/execution';
import {
    WorkflowExecution as AutomationWorkflowExecution,
    WorkflowExecutionFromJSON as AutomationWorkflowExecutionFromJSON,
} from '@/shared/middleware/automation/workflow/execution';

export interface AnyWorkflowExecutionRowI {
    automationWorkflowExecution?: AutomationWorkflowExecution;
    integrationWorkflowExecution?: EmbeddedWorkflowExecution;
    job?: AutomationWorkflowExecution['job'] | EmbeddedWorkflowExecution['job'];
    triggerExecution?: AutomationWorkflowExecution['triggerExecution'] | EmbeddedWorkflowExecution['triggerExecution'];
    workflow?: AutomationWorkflowExecution['workflow'] | EmbeddedWorkflowExecution['workflow'];
}

export const toAnyWorkflowExecutionRow = (json: object): AnyWorkflowExecutionRowI => {
    const embeddedWorkflowExecution = EmbeddedWorkflowExecutionFromJSON(json);

    if (
        embeddedWorkflowExecution.source === EmbeddedWorkflowExecutionSourceEnum.Automation &&
        embeddedWorkflowExecution.automationWorkflowExecution
    ) {
        const automationWorkflowExecution = AutomationWorkflowExecutionFromJSON(
            embeddedWorkflowExecution.automationWorkflowExecution
        );

        return {
            automationWorkflowExecution,
            job: automationWorkflowExecution.job,
            triggerExecution: automationWorkflowExecution.triggerExecution,
            workflow: automationWorkflowExecution.workflow,
        };
    }

    const integrationWorkflowExecution = embeddedWorkflowExecution.integrationWorkflowExecution as
        EmbeddedWorkflowExecution | undefined;

    return {
        integrationWorkflowExecution,
        job: integrationWorkflowExecution?.job,
        triggerExecution: integrationWorkflowExecution?.triggerExecution,
        workflow: integrationWorkflowExecution?.workflow,
    };
};
