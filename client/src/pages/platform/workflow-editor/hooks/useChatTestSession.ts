import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import getChatTriggerName from '@/pages/platform/workflow-editor/utils/getChatTriggerName';
import getSelectedTestRunTriggerName from '@/pages/platform/workflow-editor/utils/getSelectedTestRunTriggerName';
import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {useCallback, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

export default function useChatTestSession(chatTrigger?: boolean) {
    const chatSessionRef = useRef(!!chatTrigger);

    const setDataPillPanelOpen = useDataPillPanelStore((state) => state.setDataPillPanelOpen);
    const setWorkflowNodeDetailsPanelOpen = useWorkflowNodeDetailsPanelStore(
        (state) => state.setWorkflowNodeDetailsPanelOpen
    );
    const {resetMessages, setChatTriggerName, setWorkflowTestChatPanelOpen, workflowTestChatPanelOpen} =
        useWorkflowTestChatStore(
            useShallow((state) => ({
                resetMessages: state.resetMessages,
                setChatTriggerName: state.setChatTriggerName,
                setWorkflowTestChatPanelOpen: state.setWorkflowTestChatPanelOpen,
                workflowTestChatPanelOpen: state.workflowTestChatPanelOpen,
            }))
        );

    const startChatTestSession = useCallback(
        (workflow: {triggers?: Array<Pick<WorkflowTrigger, 'name' | 'type'>>}) => {
            chatSessionRef.current = true;

            setChatTriggerName(getChatTriggerName(workflow, getSelectedTestRunTriggerName(workflow)));
            resetMessages();
            setDataPillPanelOpen(false);
            setWorkflowNodeDetailsPanelOpen(false);
            setWorkflowTestChatPanelOpen(true);
        },
        [
            resetMessages,
            setChatTriggerName,
            setDataPillPanelOpen,
            setWorkflowNodeDetailsPanelOpen,
            setWorkflowTestChatPanelOpen,
        ]
    );

    const endChatTestSession = useCallback(() => {
        if (!chatSessionRef.current) {
            return false;
        }

        chatSessionRef.current = false;

        setWorkflowTestChatPanelOpen(false);

        return true;
    }, [setWorkflowTestChatPanelOpen]);

    return {chatSessionRef, endChatTestSession, startChatTestSession, workflowTestChatPanelOpen};
}
