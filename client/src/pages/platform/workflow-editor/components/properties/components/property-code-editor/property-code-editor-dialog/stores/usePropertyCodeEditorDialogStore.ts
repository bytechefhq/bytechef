import {ConnectionDeclarationType} from '@/shared/components/monaco/completion/completionTypes';
import {ScriptTestExecution} from '@/shared/middleware/graphql';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface PropertyCodeEditorStateI {
    componentConnections: ConnectionDeclarationType[];
    conversationToken: string | null;
    copilotPanelOpen: boolean;
    dirty: boolean;
    editorValue: string | undefined;
    inputParameters: Record<string, unknown> | undefined;
    rightPanelOpen: boolean;
    saving: boolean;
    scriptInput: Record<string, unknown>;
    scriptIsRunning: boolean;
    scriptTestExecution: ScriptTestExecution | undefined;
}

interface PropertyCodeEditorActionsI {
    reset: () => void;
    setConversationToken: (token: string | null) => void;
    setComponentConnections: (componentConnections: ConnectionDeclarationType[]) => void;
    setCopilotPanelOpen: (open: boolean) => void;
    setDirty: (dirty: boolean) => void;
    setEditorValue: (value: string | undefined) => void;
    setInputParameters: (inputParameters: Record<string, unknown> | undefined) => void;
    setRightPanelOpen: (open: boolean) => void;
    setSaving: (saving: boolean) => void;
    setScriptInput: (scriptInput: Record<string, unknown>) => void;
    setScriptIsRunning: (running: boolean) => void;
    setScriptTestExecution: (execution: ScriptTestExecution | undefined) => void;
}

type PropertyCodeEditorStoreType = PropertyCodeEditorActionsI & PropertyCodeEditorStateI;

const initialState: PropertyCodeEditorStateI = {
    componentConnections: [],
    conversationToken: null,
    copilotPanelOpen: false,
    dirty: false,
    editorValue: undefined,
    inputParameters: undefined,
    rightPanelOpen: true,
    saving: false,
    scriptInput: {},
    scriptIsRunning: false,
    scriptTestExecution: undefined,
};

export const usePropertyCodeEditorDialogStore = create<PropertyCodeEditorStoreType>()(
    devtools(
        (set) => ({
            ...initialState,
            reset: () => set(initialState),
            setComponentConnections: (componentConnections) => set({componentConnections}),
            setConversationToken: (token) => set({conversationToken: token}),
            setCopilotPanelOpen: (open) => set({copilotPanelOpen: open}),
            setDirty: (dirty) => set({dirty}),
            setEditorValue: (value) => set({editorValue: value}),
            setInputParameters: (inputParameters) => set({inputParameters}),
            setRightPanelOpen: (open) => set({rightPanelOpen: open}),
            setSaving: (saving) => set({saving}),
            setScriptInput: (scriptInput) => set({scriptInput}),
            setScriptIsRunning: (running) => set({scriptIsRunning: running}),
            setScriptTestExecution: (execution) => set({scriptTestExecution: execution}),
        }),
        {name: 'bytechef.property-code-editor-dialog-store'}
    )
);
