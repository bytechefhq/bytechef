import {usePropertyCodeEditorDialogStore} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useCallback, useEffect, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface UsePropertyCodeEditorDialogProps {
    language: string;
    onClose?: () => void;
    value?: string;
    workflow: Workflow;
    workflowNodeName: string;
}

export const usePropertyCodeEditorDialog = ({
    language,
    onClose,
    value,
    workflow,
    workflowNodeName,
}: UsePropertyCodeEditorDialogProps) => {
    const [unsavedChangesAlertDialogOpen, setUnsavedChangesAlertDialogOpen] = useState(false);

    const {copilotPanelOpen, dirty, editorValue, reset, setCopilotPanelOpen, setDirty, setEditorValue, setSaving} =
        usePropertyCodeEditorDialogStore(
            useShallow((state) => ({
                copilotPanelOpen: state.copilotPanelOpen,
                dirty: state.dirty,
                editorValue: state.editorValue,
                reset: state.reset,
                setCopilotPanelOpen: state.setCopilotPanelOpen,
                setDirty: state.setDirty,
                setEditorValue: state.setEditorValue,
                setSaving: state.setSaving,
            }))
        );

    const handleClose = useCallback(() => {
        reset();

        if (onClose) {
            onClose();
        }
    }, [onClose, reset]);

    const handleUnsavedChangesAlertDialogCancel = useCallback(() => {
        setUnsavedChangesAlertDialogOpen(false);
    }, []);

    const handleUnsavedChangesAlertDialogClose = useCallback(() => {
        setUnsavedChangesAlertDialogOpen(false);
        handleClose();
    }, [handleClose]);

    const setContext = useCopilotStore((state) => state.setContext);
    const ai = useApplicationInfoStore((state) => state.ai);
    const ff_1570 = useFeatureFlagsStore()('ff-1570');

    const copilotEnabled = ai.copilot.enabled && ff_1570;

    const handleCopilotClick = useCallback(() => {
        const currentContext = useCopilotStore.getState().context;

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {language},
            source: Source.CODE_EDITOR,
        });

        setCopilotPanelOpen(true);
    }, [language, setContext, setCopilotPanelOpen]);

    const handleCopilotClose = useCallback(() => {
        setCopilotPanelOpen(false);
    }, [setCopilotPanelOpen]);

    const handleOpenChange = useCallback(
        (open: boolean) => {
            if (!open && dirty) {
                setUnsavedChangesAlertDialogOpen(true);
            } else if (!open) {
                handleClose();
            }
        },
        [dirty, handleClose]
    );

    useEffect(() => {
        setEditorValue(value);
    }, [value, setEditorValue]);

    useEffect(() => {
        if (value === editorValue) {
            setDirty(false);
            setSaving(false);
        } else {
            setDirty(true);
        }
    }, [value, editorValue, setDirty, setSaving]);

    const currentWorkflowTask = getTask({
        tasks: workflow.tasks || [],
        workflowNodeName,
    });

    return {
        copilotEnabled,
        copilotPanelOpen,
        currentWorkflowTask,
        handleCopilotClick,
        handleCopilotClose,
        handleOpenChange,
        handleUnsavedChangesAlertDialogCancel,
        handleUnsavedChangesAlertDialogClose,
        unsavedChangesAlertDialogOpen,
    };
};
