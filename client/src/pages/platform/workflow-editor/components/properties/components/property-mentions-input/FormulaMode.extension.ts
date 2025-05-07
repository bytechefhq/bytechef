import {Extension} from '@tiptap/react';

export interface FormulaModeOptionsI {
    saveNullValue?: () => void;
    setIsFormulaMode: (value: boolean) => void;
}

declare module '@tiptap/core' {
    // eslint-disable-next-line @typescript-eslint/naming-convention
    interface Commands<ReturnType> {
        formulaMode: {
            toggleFormulaMode: (value: boolean) => ReturnType;
        };
    }
}

export const FormulaMode = Extension.create<FormulaModeOptionsI>({
    addCommands() {
        return {
            toggleFormulaMode: (value: boolean) => () => {
                this.storage.isFormulaMode = value;

                return true;
            },
        };
    },
    addKeyboardShortcuts() {
        return {
            Backspace: () => {
                if (this.editor.isEmpty && this.storage.isFormulaMode) {
                    this.options.setIsFormulaMode(false);

                    this.storage.isFormulaMode = false;

                    if (this.options.saveNullValue) {
                        this.options.saveNullValue();
                    }
                }

                return false;
            },
        };
    },
    addOptions() {
        return {
            saveNullValue: undefined,
            setIsFormulaMode: () => {},
        };
    },
    addStorage() {
        return {
            isFormulaMode: false,
        };
    },
    name: 'FormulaMode',
});
