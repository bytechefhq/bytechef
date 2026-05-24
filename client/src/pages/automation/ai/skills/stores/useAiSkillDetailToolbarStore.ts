import {create} from 'zustand';

interface ToolbarHandlersI {
    onCopilot: () => void;
    onDelete: () => void;
    onDownload: () => void;
    onEdit: () => void;
    onSave: () => Promise<void>;
}

interface AiSkillDetailToolbarStateI {
    canSave: boolean;
    handlers: ToolbarHandlersI | null;
    isSaving: boolean;
    resetToolbar: () => void;
    setCanSave: (canSave: boolean) => void;
    setHandlers: (handlers: ToolbarHandlersI | null) => void;
    setIsSaving: (isSaving: boolean) => void;
}

/**
 * Bridge state that lets AiSkillDetail publish its toolbar action handlers + dirty/saving flags so
 * AiSkills can render the action buttons in the page header. Lives outside the React tree (Zustand)
 * because the page header is rendered several layers above AiSkillDetail and prop-drilling would have
 * to cross LayoutContainer, the route detection, and AiSkillsPanel.
 */
const useAiSkillDetailToolbarStore = create<AiSkillDetailToolbarStateI>((set) => ({
    canSave: false,
    handlers: null,
    isSaving: false,
    resetToolbar: () => set({canSave: false, handlers: null, isSaving: false}),
    setCanSave: (canSave) => set({canSave}),
    setHandlers: (handlers) => set({handlers}),
    setIsSaving: (isSaving) => set({isSaving}),
}));

export default useAiSkillDetailToolbarStore;
