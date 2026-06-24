import {create} from 'zustand';

interface CopilotCodeToolResultStateI {
    clear: () => void;
    lastUpdatedDefinition: string | null;
    setLastUpdatedDefinition: (definition: string | null) => void;
}

const useCopilotCodeToolResultStore = create<CopilotCodeToolResultStateI>((set) => ({
    clear: () => set({lastUpdatedDefinition: null}),
    lastUpdatedDefinition: null,
    setLastUpdatedDefinition: (definition) => set({lastUpdatedDefinition: definition}),
}));

export default useCopilotCodeToolResultStore;
