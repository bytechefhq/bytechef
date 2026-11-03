import {create} from 'zustand';

interface ToolbarHandlersI {
    onCopilot: () => void;
    onDelete: () => void;
    onDownload: () => void;
    onSave: () => Promise<void>;
}

export type ViewModeType = 'preview' | 'source';

interface AiSkillDetailToolbarStateI {
    canManage: boolean;
    canSave: boolean;
    canToggleView: boolean;
    handlers: ToolbarHandlersI | null;
    isSaving: boolean;
    resetToolbar: () => void;
    setCanManage: (canManage: boolean) => void;
    setCanSave: (canSave: boolean) => void;
    setCanToggleView: (canToggleView: boolean) => void;
    setHandlers: (handlers: ToolbarHandlersI | null) => void;
    setIsSaving: (isSaving: boolean) => void;
    setViewMode: (viewMode: ViewModeType) => void;
    viewMode: ViewModeType;
}

const useAiSkillDetailToolbarStore = create<AiSkillDetailToolbarStateI>((set) => ({
    canManage: false,
    canSave: false,
    canToggleView: false,
    handlers: null,
    isSaving: false,
    resetToolbar: () =>
        set({
            canManage: false,
            canSave: false,
            canToggleView: false,
            handlers: null,
            isSaving: false,
            viewMode: 'preview',
        }),
    setCanManage: (canManage) => set({canManage}),
    setCanSave: (canSave) => set({canSave}),
    setCanToggleView: (canToggleView) => set({canToggleView}),
    setHandlers: (handlers) => set({handlers}),
    setIsSaving: (isSaving) => set({isSaving}),
    setViewMode: (viewMode) => set({viewMode}),
    viewMode: 'preview',
}));

export default useAiSkillDetailToolbarStore;
