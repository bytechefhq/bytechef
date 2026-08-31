import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect, useRef} from 'react';
import {useNavigate} from 'react-router-dom';

import {aiHubChatsStore} from '../chats/stores/useAiHubChatsStore';
import {aiHubStore} from '../stores/useAiHubStore';

/**
 * Clears the active AI Hub chat and returns to the hub home whenever the selected environment changes.
 * A chat is scoped to one environment's resources, so carrying it across a switch leaves the user on a
 * panel whose tables and files no longer exist.
 *
 * This used to hang off the `onChange` of the EnvironmentSelect rendered inside the AI Hub. Now that the
 * selector lives once in the app sidebar, the reset follows the store instead of the widget — which also
 * covers the environment being switched from anywhere else in the app, a case the old wiring missed.
 *
 * Skips the initial mount: there is no previous environment to have moved away from.
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
