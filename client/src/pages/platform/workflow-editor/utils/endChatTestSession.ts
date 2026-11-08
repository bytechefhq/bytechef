import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';

export default function endChatTestSession(): boolean {
    const {chatTriggerName, endChatTestSession: endSession} = useWorkflowTestChatStore.getState();

    if (!chatTriggerName) {
        return false;
    }

    endSession();

    return true;
}
