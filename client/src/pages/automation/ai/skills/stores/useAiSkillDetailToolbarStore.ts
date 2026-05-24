import {create} from 'zustand';

interface ToolbarHandlersI {
    onCopilot: () => void;
    onDelete: () => void;
    onDownload: () => void;
    onSave: () => Promise<void>;
}

export type ViewModeType = 'preview' | 'source';

interface AiSkillDetailToolbarStateI {
    canSave: boolean;
    canToggleView: boolean;
    handlers: ToolbarHandlersI | null;
    isSaving: boolean;
    resetToolbar: () => void;
    setCanSave: (canSave: boolean) => void;
    setCanToggleView: (canToggleView: boolean) => void;
    setHandlers: (handlers: ToolbarHandlersI | null) => void;
    setIsSaving: (isSaving: boolean) => void;
    setViewMode: (viewMode: ViewModeType) => void;
    viewMode: ViewModeType;
}

const useAiSkillDetailToolbarStore = create<AiSkillDetailToolbarStateI>((set) => ({
    canSave: false,
    canToggleView: false,
    handlers: null,
    isSaving: false,
    resetToolbar: () =>
        set({canSave: false, canToggleView: false, handlers: null, isSaving: false, viewMode: 'preview'}),
    setCanSave: (canSave) => set({canSave}),
    setCanToggleView: (canToggleView) => set({canToggleView}),
    setHandlers: (handlers) => set({handlers}),
    setIsSaving: (isSaving) => set({isSaving}),
    setViewMode: (viewMode) => set({viewMode}),
    viewMode: 'preview',
}));

export default useAiSkillDetailToolbarStore;
