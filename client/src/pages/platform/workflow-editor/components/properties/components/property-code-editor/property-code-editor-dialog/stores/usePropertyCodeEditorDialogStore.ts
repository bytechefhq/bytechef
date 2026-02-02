import {ScriptTestExecution} from '@/shared/middleware/platform/configuration';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface PropertyCodeEditorStateI {
    copilotPanelOpen: boolean;
    dirty: boolean;
    editorValue: string | undefined;
    saving: boolean;
    scriptIsRunning: boolean;
    scriptTestExecution: ScriptTestExecution | undefined;
}

interface PropertyCodeEditorActionsI {
    reset: () => void;
    setCopilotPanelOpen: (open: boolean) => void;
    setDirty: (dirty: boolean) => void;
    setEditorValue: (value: string | undefined) => void;
    setSaving: (saving: boolean) => void;
    setScriptIsRunning: (running: boolean) => void;
    setScriptTestExecution: (execution: ScriptTestExecution | undefined) => void;
}

type PropertyCodeEditorStoreType = PropertyCodeEditorActionsI & PropertyCodeEditorStateI;

const initialState: PropertyCodeEditorStateI = {
    copilotPanelOpen: false,
    dirty: false,
    editorValue: undefined,
    saving: false,
    scriptIsRunning: false,
    scriptTestExecution: undefined,
};

export const usePropertyCodeEditorDialogStore = create<PropertyCodeEditorStoreType>()(
    devtools(
        (set) => ({
            ...initialState,
            reset: () => set(initialState),
            setCopilotPanelOpen: (open) => set({copilotPanelOpen: open}),
            setDirty: (dirty) => set({dirty}),
            setEditorValue: (value) => set({editorValue: value}),
            setSaving: (saving) => set({saving}),
            setScriptIsRunning: (running) => set({scriptIsRunning: running}),
            setScriptTestExecution: (execution) => set({scriptTestExecution: execution}),
        }),
        {name: 'bytechef.property-code-editor-store'}
    )
);
