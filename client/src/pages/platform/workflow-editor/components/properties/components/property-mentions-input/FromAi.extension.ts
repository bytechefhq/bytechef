import {Extension} from '@tiptap/core';

export interface FromAiOptionsI {
    setFromAi: (value: boolean) => void;
}

declare module '@tiptap/core' {
    // eslint-disable-next-line @typescript-eslint/naming-convention
    interface Commands<ReturnType> {
        fromAi: {
            setFromAi: (value: boolean) => ReturnType;
        };
    }
}

export const FromAi = Extension.create<FromAiOptionsI>({
    addCommands() {
        return {
            setFromAi: (value: boolean) => () => {
                this.storage.fromAi = value;

                return true;
            },
        };
    },
    addOptions() {
        return {
            setFromAi: () => {},
        };
    },
    addStorage() {
        return {
            fromAi: false,
        };
    },
    name: 'fromAi',
});
