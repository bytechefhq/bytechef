import isChatTriggerType from '@/pages/platform/workflow-editor/utils/isChatTriggerType';
import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';

export default function getChatTriggerName(
    workflow: {triggers?: Array<Pick<WorkflowTrigger, 'name' | 'type'>>},
    preferredTriggerName?: string
) {
    const chatTriggers = workflow.triggers?.filter((trigger) => isChatTriggerType(trigger.type));

    const chatTrigger = chatTriggers?.find((trigger) => trigger.name === preferredTriggerName) || chatTriggers?.[0];

    return chatTrigger?.name || 'trigger_1';
}
