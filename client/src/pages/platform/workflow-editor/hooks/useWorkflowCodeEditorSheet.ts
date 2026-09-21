import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {resolveAutoApplyDefinition} from '@/shared/components/copilot/utils/resolveAutoApplyDefinition';
import {usePersistJobId} from '@/shared/hooks/usePersistJobId';
import {useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {useValidateWorkflowQuery} from '@/shared/middleware/graphql';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {WorkflowTestApi, WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {WorkflowDefinitionType} from '@/shared/types';
import {getTestWorkflowAttachRequest, getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/shallow';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import getWorkflowCodeEditorIssueMessages from '../utils/getWorkflowCodeEditorIssueMessages';
import saveWorkflowDefinitionUpdate from '../utils/saveWorkflowDefinitionUpdate';

import type {editor} from 'monaco-editor';

const workflowTestApi = new WorkflowTestApi();

const MARKER_SEVERITY_ERROR = 8;

const APPLIED_TO_EDITOR_MESSAGE = '✓ Applied changes to the editor.';

type UseWorkflowCodeEditorSheetReturnType = {
    copilotEnabled: boolean;
    copilotPanelOpen: boolean;
    definition: string;
    dirty: boolean;
    errors: string[];
    errorsAccordionOpen: boolean;
    handleCopilotClick: () => void;
    handleCopilotClose: () => void;
    handleDefinitionChange: (value: string) => void;
    handleOpenChange: (open: boolean) => void;
    handleRunClick: () => void;
    handleSaveClick: (workflow: Workflow, definition: string) => void;
    handleStopClick: () => void;
    handleUnsavedChangesAlertDialogClose: () => void;
    handleUnsavedChangesAlertDialogOpen: (open: boolean) => void;
    handleValidate: (markers: editor.IMarkerData[]) => void;
    handleWorkflowTestConfigurationDialog: (open: boolean) => void;
    hasErrors: boolean;
    projectName: string | null;
    setErrorsAccordionOpen: (open: boolean) => void;
    setWarningsAccordionOpen: (open: boolean) => void;
    showWorkflowTestConfigurationDialog: boolean;
    unsavedChangesAlertDialogOpen: boolean;
    warnings: string[];
    warningsAccordionOpen: boolean;
    workflowIsRunning: boolean;
    workflowTestExecution: WorkflowTestExecution | undefined;
};

interface UseWorkflowCodeEditorSheetProps {
    invalidateWorkflowQueries: () => void;
    onSheetOpenClose: (open: boolean) => void;
    workflow: Workflow;
}

const useWorkflowCodeEditorSheet = ({
    invalidateWorkflowQueries,
    onSheetOpenClose,
    workflow,
}: UseWorkflowCodeEditorSheetProps): UseWorkflowCodeEditorSheetReturnType => {
    const [copilotPanelOpen, setCopilotPanelOpen] = useState(false);
    const [definition, setDefinition] = useState<string>(workflow.definition!);
    const [dirty, setDirty] = useState<boolean>(false);
    const [errorsAccordionOpen, setErrorsAccordionOpen] = useState(false);
    const [warningsAccordionOpen, setWarningsAccordionOpen] = useState(false);
    const [jobId, setJobId] = useState<string | null>(null);
    const [showWorkflowTestConfigurationDialog, setShowWorkflowTestConfigurationDialog] = useState(false);
    const [unsavedChangesAlertDialogOpen, setUnsavedChangesAlertDialogOpen] = useState(false);
    const [workflowIsRunning, setWorkflowIsRunning] = useState(false);
    const [workflowTestExecution, setWorkflowTestExecution] = useState<WorkflowTestExecution>();
    const [markers, setMarkers] = useState<editor.IMarkerData[]>([]);

    const conversationTokenRef = useRef<string | null>(null);

    const hasErrors = markers.some((marker) => marker.severity === MARKER_SEVERITY_ERROR);

    const ai = useApplicationInfoStore((state) => state.ai);
    const setContext = useCopilotStore((state) => state.setContext);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const setShowBottomPanelOpen = useWorkflowEditorStore((state) => state.setShowBottomPanelOpen);

    const {projectName} = useWorkflowDataStore(
        useShallow((state) => ({
            projectName: state.projectName,
        }))
    );

    const ff_1570 = useFeatureFlagsStore()('ff-1570');

    const copilotEnabled = ai.copilot.enabled && ff_1570;

    const {getPersistedJobId, persistJobId} = usePersistJobId(workflow.id, currentEnvironmentId);
    const {close: closeWorkflowTestStream, setStreamRequest} = useWorkflowTestStream({
        onError: () => {
            setWorkflowTestExecution(undefined);
            setWorkflowIsRunning(false);
            setJobId(null);
        },
        onResult: (execution) => {
            setWorkflowTestExecution(execution);
            setWorkflowIsRunning(false);
            setJobId(null);
            setShowBottomPanelOpen(true);
        },
        onStart: (jobId) => setJobId(jobId),
        workflowId: workflow.id!,
    });
    const {updateWorkflowMutation} = useWorkflowEditor();

    const handleCopilotClick = useCallback(() => {
        const {
            context: currentContext,
            generateConversationId,
            resetMessages,
            saveConversationState,
        } = useCopilotStore.getState();

        conversationTokenRef.current = saveConversationState();
        resetMessages();
        generateConversationId();

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {format: workflow.format?.toLowerCase() ?? 'json'},
            source: Source.WORKFLOW_CODE_EDITOR,
        });

        setCopilotPanelOpen(true);
    }, [setContext, workflow.format]);

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState(conversationTokenRef.current);
        setCopilotPanelOpen(false);
    }, []);

    const handleOpenChange = useCallback(
        (open: boolean) => {
            if (!open && dirty) {
                setUnsavedChangesAlertDialogOpen(true);

                return;
            }

            if (!open) {
                useCopilotStore.getState().restoreConversationState(conversationTokenRef.current);
                setCopilotPanelOpen(false);
            }

            onSheetOpenClose(open);
        },
        [dirty, onSheetOpenClose]
    );

    const handleRunClick = () => {
        setWorkflowTestExecution(undefined);
        setWorkflowIsRunning(true);
        setJobId(null);
        persistJobId(null);

        if (workflow?.id) {
            const request = getTestWorkflowStreamPostRequest({
                environmentId: currentEnvironmentId,
                id: workflow.id,
            });

            setStreamRequest(request);
        }
    };

    const handleSaveClick = (workflow: Workflow, definition: string) => {
        if (workflow && workflow.id) {
            let editedWorkflowDefinition: WorkflowDefinitionType;

            try {
                editedWorkflowDefinition = JSON.parse(definition);
            } catch (error) {
                console.error(`Invalid JSON: ${error}`);

                return;
            }

            saveWorkflowDefinitionUpdate({
                onError: () => setDirty(true),
                onSuccess: () => {
                    setDirty(false);

                    invalidateWorkflowQueries();
                },
                updateDefinition: () => editedWorkflowDefinition,
                updateWorkflowMutation: updateWorkflowMutation!,
            });
        }
    };

    const handleStopClick = useCallback(() => {
        setWorkflowIsRunning(false);
        setStreamRequest(null);
        closeWorkflowTestStream();

        if (jobId) {
            workflowTestApi.stopWorkflowTest({jobId}, {keepalive: true}).finally(() => {
                persistJobId(null);
                setJobId(null);
            });
        }
    }, [closeWorkflowTestStream, jobId, persistJobId, setStreamRequest]);

    const handleDefinitionChange = useCallback(
        (value: string) => {
            setDefinition(value);

            setDirty(value !== workflow.definition);
        },
        [workflow.definition]
    );

    const {data: validateWorkflowData, refetch: refetchValidateWorkflow} = useValidateWorkflowQuery(
        {environmentId: currentEnvironmentId, workflowDefinition: definition!, workflowId: workflow.id},
        {enabled: !!definition}
    );

    const {errors, warnings} = useMemo(
        () =>
            getWorkflowCodeEditorIssueMessages({
                definition: definition ?? '',
                errors: validateWorkflowData?.validateWorkflow.errors ?? [],
                nodeIssues: validateWorkflowData?.validateWorkflow.nodeIssues ?? [],
                warnings: validateWorkflowData?.validateWorkflow.warnings ?? [],
            }),
        [definition, validateWorkflowData]
    );

    const handleValidate = useCallback(
        (newMarkers: editor.IMarkerData[]) => {
            setMarkers(newMarkers);

            refetchValidateWorkflow();
        },
        [refetchValidateWorkflow]
    );

    const handleUnsavedChangesAlertDialogClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState(conversationTokenRef.current);
        setCopilotPanelOpen(false);
        setUnsavedChangesAlertDialogOpen(false);
        onSheetOpenClose(false);
    }, [onSheetOpenClose]);

    useEffect(() => {
        return useCopilotPostTurnRegistry.getState().register(Source.WORKFLOW_CODE_EDITOR, () => {
            const {appendToLastAssistantMessage, context, messages} = useCopilotStore.getState();

            const definition = resolveAutoApplyDefinition(Source.WORKFLOW_CODE_EDITOR, context?.mode, messages);

            if (!definition) {
                return;
            }

            handleDefinitionChange(definition);

            appendToLastAssistantMessage(APPLIED_TO_EDITOR_MESSAGE);
        });
    }, [handleDefinitionChange]);

    useEffect(() => {
        setDefinition(workflow.definition!);
    }, [workflow.definition]);

    useEffect(() => {
        if (!workflow.id || currentEnvironmentId === undefined) {
            return;
        }

        const jobId = getPersistedJobId();

        if (!jobId) {
            return;
        }

        setWorkflowIsRunning(true);
        setJobId(jobId);

        setStreamRequest(getTestWorkflowAttachRequest({jobId}));
    }, [workflow.id, currentEnvironmentId, getPersistedJobId, setWorkflowIsRunning, setJobId, setStreamRequest]);

    return {
        copilotEnabled,
        copilotPanelOpen,
        definition,
        dirty,
        errors,
        errorsAccordionOpen,
        handleCopilotClick,
        handleCopilotClose,
        handleDefinitionChange,
        handleOpenChange,
        handleRunClick,
        handleSaveClick,
        handleStopClick,
        handleUnsavedChangesAlertDialogClose,
        handleUnsavedChangesAlertDialogOpen: setUnsavedChangesAlertDialogOpen,
        handleValidate,
        handleWorkflowTestConfigurationDialog: setShowWorkflowTestConfigurationDialog,
        hasErrors,
        projectName,
        setErrorsAccordionOpen,
        setWarningsAccordionOpen,
        showWorkflowTestConfigurationDialog,
        unsavedChangesAlertDialogOpen,
        warnings,
        warningsAccordionOpen,
        workflowIsRunning,
        workflowTestExecution,
    };
};

export default useWorkflowCodeEditorSheet;
