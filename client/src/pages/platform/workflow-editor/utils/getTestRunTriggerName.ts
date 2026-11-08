import isTriggerEnabled from '@/pages/platform/workflow-editor/utils/isTriggerEnabled';
import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';

export default function getTestRunTriggerName(
    workflow: {triggers?: Array<Pick<WorkflowTrigger, 'disabled' | 'name' | 'type'>>},
    selectedNodeName?: string
): string {
    const selectedTrigger = workflow.triggers?.find((trigger) => trigger.name === selectedNodeName);

    if (selectedTrigger && isTriggerEnabled(selectedTrigger)) {
        return selectedTrigger.name;
    }

    const firstEnabledTrigger = workflow.triggers?.find((trigger) => isTriggerEnabled(trigger));

    return firstEnabledTrigger?.name || workflow.triggers?.[0]?.name || 'trigger_1';
}
