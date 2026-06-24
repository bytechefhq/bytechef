import {create} from 'zustand';

type ToolResultHandlerType = (content: string) => void;

interface ToolResultHandlerRegistryStateI {
    handlers: Partial<Record<string, ToolResultHandlerType>>;
    register: (toolName: string, handler: ToolResultHandlerType) => () => void;
    runFor: (toolName: string, content: string) => void;
}

const useCopilotToolResultHandlerRegistry = create<ToolResultHandlerRegistryStateI>((set, get) => ({
    handlers: {},
    register: (toolName, handler) => {
        set((state) => ({handlers: {...state.handlers, [toolName]: handler}}));

        return () => {
            set((state) => {
                if (state.handlers[toolName] !== handler) {
                    return state;
                }

                const nextHandlers = {...state.handlers};

                delete nextHandlers[toolName];

                return {handlers: nextHandlers};
            });
        };
    },
    runFor: (toolName, content) => {
        get().handlers[toolName]?.(content);
    },
}));

export default useCopilotToolResultHandlerRegistry;
