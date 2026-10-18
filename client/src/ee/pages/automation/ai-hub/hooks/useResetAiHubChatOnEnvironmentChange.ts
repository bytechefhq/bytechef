import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect, useRef} from 'react';
import {useNavigate} from 'react-router-dom';

import {aiHubChatsStore} from '../chats/stores/useAiHubChatsStore';
import {aiHubStore} from '../stores/useAiHubStore';

/**
 * Clears the active AI Hub chat and returns to the hub home whenever the selected environment changes.
 * Skips the initial mount.
 */
export function useResetAiHubChatOnEnvironmentChange(): void {
    const isFirstRunRef = useRef(true);
    const previousEnvironmentIdRef = useRef<number | undefined>(undefined);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const navigate = useNavigate();

    useEffect(() => {
        if (isFirstRunRef.current) {
            isFirstRunRef.current = false;
            previousEnvironmentIdRef.current = currentEnvironmentId;

            return;
        }

        const previousEnvironmentId = previousEnvironmentIdRef.current;

        previousEnvironmentIdRef.current = currentEnvironmentId;

        if (previousEnvironmentId === currentEnvironmentId) {
            return;
        }

        aiHubChatsStore.getState().setCurrentChatId(undefined);

        aiHubStore.getState().resetMessages();
        aiHubStore.getState().generateChatId();

        navigate('/automation/ai-hub');
    }, [currentEnvironmentId, navigate]);
}
