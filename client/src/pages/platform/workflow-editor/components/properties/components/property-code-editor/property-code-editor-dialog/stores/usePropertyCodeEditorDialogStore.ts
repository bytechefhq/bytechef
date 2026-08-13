import {ScriptTestExecution} from '@/shared/middleware/graphql';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface PropertyCodeEditorStateI {
    // Token minted by the last saveConversationState() call the toolbar's handleCopilotClick made. The
    // toolbar (which opens Copilot) and this dialog (which restores on close) are sibling hooks with no
    // shared component instance, so a local ref can't carry the token between them the way it does for the
    // other local-panel surfaces — this store is the shared place both can reach.
    conversationToken: string | null;
    copilotPanelOpen: boolean;
    dirty: boolean;
    editorValue: string | undefined;
    inputParameters: Record<string, unknown> | undefined;
    rightPanelOpen: boolean;
    saving: boolean;
    scriptIsRunning: boolean;
    scriptTestExecution: ScriptTestExecution | undefined;
}

interface PropertyCodeEditorActionsI {
    reset: () => void;
    setConversationToken: (token: string | null) => void;
    setCopilotPanelOpen: (open: boolean) => void;
    setDirty: (dirty: boolean) => void;
    setEditorValue: (value: string | undefined) => void;
    setInputParameters: (inputParameters: Record<string, unknown> | undefined) => void;
    setRightPanelOpen: (open: boolean) => void;
    setSaving: (saving: boolean) => void;
    setScriptIsRunning: (running: boolean) => void;
    setScriptTestExecution: (execution: ScriptTestExecution | undefined) => void;
}

type PropertyCodeEditorStoreType = PropertyCodeEditorActionsI & PropertyCodeEditorStateI;

const initialState: PropertyCodeEditorStateI = {
    conversationToken: null,
    copilotPanelOpen: false,
    dirty: false,
    editorValue: undefined,
    inputParameters: undefined,
    rightPanelOpen: true,
    saving: false,
    scriptIsRunning: false,
    scriptTestExecution: undefined,
};

export const usePropertyCodeEditorDialogStore = create<PropertyCodeEditorStoreType>()(
    devtools(
        (set) => ({
            ...initialState,
            reset: () => set(initialState),
            setConversationToken: (token) => set({conversationToken: token}),
            setCopilotPanelOpen: (open) => set({copilotPanelOpen: open}),
            setDirty: (dirty) => set({dirty}),
            setEditorValue: (value) => set({editorValue: value}),
            setInputParameters: (inputParameters) => set({inputParameters}),
            setRightPanelOpen: (open) => set({rightPanelOpen: open}),
            setSaving: (saving) => set({saving}),
            setScriptIsRunning: (running) => set({scriptIsRunning: running}),
            setScriptTestExecution: (execution) => set({scriptTestExecution: execution}),
        }),
        {name: 'bytechef.property-code-editor-dialog-store'}
    )
);
