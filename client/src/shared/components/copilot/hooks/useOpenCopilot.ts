import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {
    CopilotContextParametersType,
    MODE,
    Source,
    useCopilotStore,
} from '@/shared/components/copilot/stores/useCopilotStore';

export interface UseOpenCopilotOptionsI {
    composerPlaceholder?: string;
    mode?: MODE;
    parameters?: CopilotContextParametersType;
    source: Source;
}

const useOpenCopilot = () => {
    const setContext = useCopilotStore((state) => state.setContext);
    const setComposerPlaceholder = useCopilotStore((state) => state.setComposerPlaceholder);
    const copilotPanelOpen = useCopilotPanelStore((state) => state.copilotPanelOpen);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);

    return ({composerPlaceholder, mode = MODE.ASK, parameters = {}, source}: UseOpenCopilotOptionsI) => {
        const {generateConversationId, resetMessages, saveConversationState, setGlobalPanelConversationToken} =
            useCopilotStore.getState();

        if (!copilotPanelOpen) {
            const token = saveConversationState();

            setGlobalPanelConversationToken(token);
        }

        resetMessages();
        generateConversationId();

        setContext({
            mode,
            parameters,
            source,
        });

        setComposerPlaceholder(composerPlaceholder);

        setCopilotPanelOpen(true);
    };
};

export default useOpenCopilot;
