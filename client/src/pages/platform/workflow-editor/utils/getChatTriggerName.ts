import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';

const CHAT_TRIGGER_TYPE_PATTERN = /^chat\/v\d+\/newChatRequest$/;

export default function getChatTriggerName(workflow: {triggers?: Array<Pick<WorkflowTrigger, 'name' | 'type'>>}) {
    const chatTrigger = workflow.triggers?.find((trigger) => CHAT_TRIGGER_TYPE_PATTERN.test(trigger.type));

    return chatTrigger?.name || 'trigger_1';
}
