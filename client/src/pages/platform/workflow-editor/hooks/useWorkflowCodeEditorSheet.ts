import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {usePersistJobId} from '@/shared/hooks/usePersistJobId';
import {useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {WorkflowTestApi, WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {getTestWorkflowAttachRequest, getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
import {useCallback, useEffect, useState} from 'react';

const workflowTestApi = new WorkflowTestApi();

interface UseWorkflowCodeEditorSheetProps {
    invalidateWorkflowQueries: () => void;
    onSheetOpenClose: (open: boolean) => void;
    workflow: Workflow;
}

const useWorkflowCodeEditorSheet = ({
    invalidateWorkflowQueries,
    onSheetOpenClose,
    workflow,
}: UseWorkflowCodeEditorSheetProps) => {
    const [copilotPanelOpen, setCopilotPanelOpen] = useState(false);
    const [definition, setDefinition] = useState<string>(workflow.definition!);
    const [dirty, setDirty] = useState<boolean>(false);
    const [jobId, setJobId] = useState<string | null>(null);
    const [showWorkflowTestConfigurationDialog, setShowWorkflowTestConfigurationDialog] = useState(false);
    const [unsavedChangesAlertDialogOpen, setUnsavedChangesAlertDialogOpen] = useState(false);
    const [workflowIsRunning, setWorkflowIsRunning] = useState(false);
    const [workflowTestExecution, setWorkflowTestExecution] = useState<WorkflowTestExecution>();

    const ai = useApplicationInfoStore((state) => state.ai);
    const setContext = useCopilotStore((state) => state.setContext);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

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

        saveConversationState();
        resetMessages();
        generateConversationId();

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {language: 'json'},
            source: Source.CODE_EDITOR,
        });

        setCopilotPanelOpen(true);
    }, [setContext]);

    const handleCopilotClose = useCallback(() => {
        useCopilotStore.getState().restoreConversationState();
        setCopilotPanelOpen(false);
    }, []);

    const handleOpenChange = useCallback(
        (open: boolean) => {
            if (!open) {
                useCopilotStore.getState().restoreConversationState();
                setCopilotPanelOpen(false);
            }

            if (!open && dirty) {
                setUnsavedChangesAlertDialogOpen(true);
            } else {
                onSheetOpenClose(open);
            }
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
            try {
                JSON.parse(definition);

                updateWorkflowMutation!.mutate(
                    {
                        id: workflow.id,
                        workflow: {
                            definition,
                            version: workflow.version,
                        },
                    },
                    {
                        onError: () => setDirty(true),
                        onSuccess: () => {
                            setDirty(false);

                            invalidateWorkflowQueries();
                        },
                    }
                );
            } catch (error) {
                console.error(`Invalid JSON: ${error}`);
            }
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

    const handleUnsavedChangesAlertDialogClose = useCallback(() => {
        setUnsavedChangesAlertDialogOpen(false);
        onSheetOpenClose(false);
    }, [onSheetOpenClose]);

    useEffect(() => {
        if (!workflow.id || currentEnvironmentId === undefined) return;

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
        handleCopilotClick,
        handleCopilotClose,
        handleDefinitionChange,
        handleOpenChange,
        handleRunClick,
        handleSaveClick,
        handleStopClick,
        handleUnsavedChangesAlertDialogClose,
        handleUnsavedChangesAlertDialogOpen: setUnsavedChangesAlertDialogOpen,
        handleWorkflowTestConfigurationDialog: setShowWorkflowTestConfigurationDialog,
        showWorkflowTestConfigurationDialog,
        unsavedChangesAlertDialogOpen,
        workflowIsRunning,
        workflowTestExecution,
    };
};

export default useWorkflowCodeEditorSheet;
