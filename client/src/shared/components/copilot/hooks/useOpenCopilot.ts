import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';

export interface UseOpenCopilotOptionsI {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters?: Record<string, any>;
    source: Source;
}

const useOpenCopilot = () => {
    const setContext = useCopilotStore((state) => state.setContext);
    const copilotPanelOpen = useCopilotPanelStore((state) => state.copilotPanelOpen);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);

    return ({parameters = {}, source}: UseOpenCopilotOptionsI) => {
        const {generateConversationId, resetMessages, saveConversationState, setGlobalPanelConversationToken} =
            useCopilotStore.getState();

        if (!copilotPanelOpen) {
            const token = saveConversationState();

            setGlobalPanelConversationToken(token);
        }

        resetMessages();
        generateConversationId();

        setContext({
            mode: MODE.ASK,
            parameters,
            source,
        });

        setCopilotPanelOpen(true);
    };
};

export default useOpenCopilot;
