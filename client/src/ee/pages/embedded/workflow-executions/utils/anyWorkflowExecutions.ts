import {
    EmbeddedWorkflowExecutionFromJSON,
    EmbeddedWorkflowExecutionSourceEnum,
    WorkflowExecution as EmbeddedWorkflowExecution,
} from '@/ee/shared/middleware/embedded/workflow/execution';
import {
    WorkflowExecution as AutomationExecution,
    WorkflowExecutionFromJSON as AutomationExecutionFromJSON,
} from '@/shared/middleware/automation/workflow/execution';

export interface AnyWorkflowExecutionRowI {
    automationExecution?: AutomationExecution;
    integrationWorkflowExecution?: EmbeddedWorkflowExecution;
    job?: AutomationExecution['job'] | EmbeddedWorkflowExecution['job'];
    triggerExecution?: AutomationExecution['triggerExecution'] | EmbeddedWorkflowExecution['triggerExecution'];
    workflow?: AutomationExecution['workflow'] | EmbeddedWorkflowExecution['workflow'];
}

export const toAnyWorkflowExecutionRow = (json: object): AnyWorkflowExecutionRowI => {
    const embeddedWorkflowExecution = EmbeddedWorkflowExecutionFromJSON(json);

    if (
        embeddedWorkflowExecution.source === EmbeddedWorkflowExecutionSourceEnum.Automation &&
        embeddedWorkflowExecution.automationExecution
    ) {
        const automationExecution = AutomationExecutionFromJSON(embeddedWorkflowExecution.automationExecution);

        return {
            automationExecution,
            job: automationExecution.job,
            triggerExecution: automationExecution.triggerExecution,
            workflow: automationExecution.workflow,
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
