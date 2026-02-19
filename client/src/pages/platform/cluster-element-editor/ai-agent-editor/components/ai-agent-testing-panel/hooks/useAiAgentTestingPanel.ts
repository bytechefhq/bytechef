import {useCallback} from 'react';

import {useAiAgentTestingChatStore, useTestingModeStore} from '../../../stores';

interface UseAiAgentTestingPanelI {
    conversationId: string | undefined;
    handleReset: () => void;
    handleTestAgent: () => void;
    isTestingAgent: boolean;
}

export default function useAiAgentTestingPanel(): UseAiAgentTestingPanelI {
    const {isTestingAgent, setIsTestingAgent} = useTestingModeStore();
    const {conversationId, generateConversationId, resetMessages} = useAiAgentTestingChatStore();

    const handleReset = useCallback(() => {
        resetMessages();
        generateConversationId();
    }, [generateConversationId, resetMessages]);

    const handleTestAgent = useCallback(() => {
        resetMessages();
        generateConversationId();
        setIsTestingAgent(true);
    }, [generateConversationId, resetMessages, setIsTestingAgent]);

    return {
        conversationId,
        handleReset,
        handleTestAgent,
        isTestingAgent,
    };
}
