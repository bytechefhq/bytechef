import {usePropertyCodeEditorDialogStore} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import {parseJson} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import useCopilotCodeToolResultStore from '@/shared/components/copilot/stores/useCopilotCodeToolResultStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import useCopilotToolResultHandlerRegistry from '@/shared/components/copilot/stores/useCopilotToolResultHandlerRegistry';
import {extractScriptFromDefinition} from '@/shared/components/copilot/utils/extractScriptFromDefinition';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {useCallback, useEffect, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

const APPLIED_TO_EDITOR_MESSAGE = '✓ Applied changes to the editor.';

interface UsePropertyCodeEditorDialogProps {
    onClose?: () => void;
    value?: string;
    workflow: Workflow;
    workflowNodeName: string;
}

export const usePropertyCodeEditorDialog = ({
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
        useCopilotStore.getState().restoreConversationState();
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

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState();
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

    useEffect(() => {
        const unregisterToolResult = useCopilotToolResultHandlerRegistry
            .getState()
            .register('updateScriptComponentCode', (content) => {
                const result = parseJson<{definition?: string}>(content, 'updateScriptComponentCode result');

                if (result?.definition) {
                    useCopilotCodeToolResultStore.getState().setLastUpdatedDefinition(result.definition);
                }
            });

        const unregisterPostTurn = useCopilotPostTurnRegistry.getState().register(Source.CODE_EDITOR, () => {
            const {appendToLastAssistantMessage, context} = useCopilotStore.getState();

            const definition = useCopilotCodeToolResultStore.getState().lastUpdatedDefinition;

            useCopilotCodeToolResultStore.getState().clear();

            if (context?.mode !== MODE.BUILD || definition == null) {
                return;
            }

            const code = extractScriptFromDefinition(definition, workflowNodeName);

            if (code == null) {
                return;
            }

            setEditorValue(code);

            appendToLastAssistantMessage(APPLIED_TO_EDITOR_MESSAGE);
        });

        return () => {
            unregisterToolResult();
            unregisterPostTurn();
        };
    }, [setEditorValue, workflowNodeName]);

    const currentWorkflowTask = getTask({
        tasks: workflow.tasks || [],
        workflowNodeName,
    });

    return {
        copilotPanelOpen,
        currentWorkflowTask,
        handleCopilotClose,
        handleOpenChange,
        handleUnsavedChangesAlertDialogCancel,
        handleUnsavedChangesAlertDialogClose,
        unsavedChangesAlertDialogOpen,
    };
};
