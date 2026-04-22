import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface CopilotPanelStateI {
    copilotPanelOpen: boolean;
    setCopilotPanelOpen: (open: boolean) => void;
}

const useCopilotPanelStore = create<CopilotPanelStateI>()(
    devtools((set) => ({
        copilotPanelOpen: false,
        setCopilotPanelOpen: (copilotPanelOpen) => set({copilotPanelOpen}),
    }))
);

export const openCopilotForFiles = () => {
    const {generateConversationId, resetMessages, saveConversationState, setContext} = useCopilotStore.getState();

    saveConversationState();
    resetMessages();
    generateConversationId();

    setContext({
        mode: MODE.ASK,
        parameters: {},
        source: Source.FILES,
    });

    useCopilotPanelStore.getState().setCopilotPanelOpen(true);
};

export default useCopilotPanelStore;
