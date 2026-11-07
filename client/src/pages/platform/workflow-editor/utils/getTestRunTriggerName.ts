import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';

export default function getTestRunTriggerName(
    workflow: {triggers?: Array<Pick<WorkflowTrigger, 'name'>>},
    selectedNodeName?: string
) {
    const selectedTrigger = workflow.triggers?.find((trigger) => trigger.name === selectedNodeName);

    return selectedTrigger?.name || workflow.triggers?.[0]?.name || 'trigger_1';
}
