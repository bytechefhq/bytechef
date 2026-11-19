import {ProjectDeploymentWorkflow, Workflow} from '@/shared/middleware/automation/configuration';

export const canRunWorkflowManually = (workflow: Workflow) =>
    !workflow.triggers?.length || workflow.triggers.some((trigger) => trigger.type.includes('manual'));

export const hasWorkflowTriggerUrl = (projectDeploymentWorkflow?: ProjectDeploymentWorkflow) =>
    !!projectDeploymentWorkflow?.staticWebhookUrl;
