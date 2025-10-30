import {Extension} from '@tiptap/core';

export interface FromAIOptionsI {
    setFromAI: (value: boolean) => void;
}

declare module '@tiptap/core' {
    // eslint-disable-next-line @typescript-eslint/naming-convention
    interface Commands<ReturnType> {
        fromAI: {
            setFromAI: (value: boolean) => ReturnType;
        };
    }
}

export const FromAI = Extension.create<FromAIOptionsI>({
    addCommands() {
        return {
            setFromAI: (value: boolean) => () => {
                this.storage.fromAI = value;

                return true;
            },
        };
    },
    addOptions() {
        return {
            setFromAI: () => {},
        };
    },
    addStorage() {
        return {
            fromAI: false,
        };
    },
    name: 'fromAI',
});
