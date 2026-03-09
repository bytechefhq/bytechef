import {Extension} from '@tiptap/core';

export interface FormulaModeOptionsI {
    getIsFormulaMode?: () => boolean;
    initialFormulaMode?: boolean;
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
    // eslint-disable-next-line @typescript-eslint/naming-convention
    interface Storage {
        FormulaMode: {
            isFormulaMode: boolean;
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
                const hasNoContent = this.editor.state.doc.textContent.trim() === '';
                const currentFormulaMode = this.options.getIsFormulaMode?.() ?? this.storage.isFormulaMode;

                if (hasNoContent && currentFormulaMode) {
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
            getIsFormulaMode: undefined,
            initialFormulaMode: false,
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
    onCreate() {
        this.storage.isFormulaMode = this.options.initialFormulaMode ?? false;
    },
});
