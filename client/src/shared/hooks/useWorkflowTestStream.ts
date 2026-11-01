import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import buildNodeStatesFromTaskExecutions from '@/pages/platform/workflow-editor/utils/buildNodeStatesFromTaskExecutions';
import getTestRunTriggerName from '@/pages/platform/workflow-editor/utils/getTestRunTriggerName';
import toClusterElementNodeState from '@/pages/platform/workflow-editor/utils/toClusterElementNodeState';
import {usePersistJobId} from '@/shared/hooks/usePersistJobId';
import {SSERequestType, useSSE} from '@/shared/hooks/useSSE';
import {
    ClusterElementCallRecord,
    ClusterElementCallRecordFromJSON,
    JobStatusEnum,
    WorkflowTestExecution,
} from '@/shared/middleware/platform/workflow/test';
import {WorkflowTestExecutionFromJSON} from '@/shared/middleware/platform/workflow/test/models/WorkflowTestExecution';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {AskUserQuestionEventI, formatAskUserQuestionMessage} from '@/shared/util/assistant-message-utils';
import {extractStreamChunk} from '@/shared/util/stream-utils';
import {useCallback, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

export interface UseWorkflowTestStreamProps {
    workflowId: string;
    onResult?: (execution: WorkflowTestExecution) => void;
    onError?: (errorMessage?: string) => void;
    onStart?: (jobId: string) => void;
    onStreamEnd?: () => void;
}

interface TaskLifecyclePayloadI {
    callEndDate?: string;
    callError?: string;
    callId?: string;
    callStartDate?: string;
    callStatus?: string;
    componentName?: string;
    endDate?: string;
    error?: string;
    input?: string;
    name?: string;
    output?: string;
    parentCallId?: string;
    startDate?: string;
    status?: string;
    taskExecutionId?: string;
    toolName?: string;
}

function computeDurationMillis(startDate?: string, endDate?: string): number | undefined {
    if (!startDate || !endDate) {
        return undefined;
    }

    const startMillis = Date.parse(startDate);
    const endMillis = Date.parse(endDate);

    if (Number.isNaN(startMillis) || Number.isNaN(endMillis) || endMillis < startMillis) {
        return undefined;
    }

    return endMillis - startMillis;
}

function clearRunningNodeStates() {
    const {removeWorkflowTestNodeState, workflowTestNodeStates} = useWorkflowEditorStore.getState();

    for (const [name, nodeState] of Object.entries(workflowTestNodeStates)) {
        if (nodeState.status === 'RUNNING') {
            removeWorkflowTestNodeState(name);
        }
    }
}

function clearProgressSnapshot() {
    const {setWorkflowTestExecution, workflowTestExecution} = useWorkflowEditorStore.getState();

    const jobStatus = workflowTestExecution?.job?.status;

    if (jobStatus === JobStatusEnum.Created || jobStatus === JobStatusEnum.Started) {
        setWorkflowTestExecution(undefined);
    }
}

function backfillNodeStatesFromResult(workflowTestExecution: WorkflowTestExecution) {
    const {setWorkflowTestNodeState, workflowTestNodeStates} = useWorkflowEditorStore.getState();

    const resultNodeStates = buildNodeStatesFromTaskExecutions(workflowTestExecution.job?.taskExecutions, false);

    for (const [name, nodeState] of Object.entries(resultNodeStates)) {
        const existingNodeState = workflowTestNodeStates[name];

        if (existingNodeState?.status === 'COMPLETED' && nodeState.status === 'FAILED') {
            setWorkflowTestNodeState(name, {
                ...existingNodeState,
                durationMillis: nodeState.durationMillis ?? existingNodeState.durationMillis,
                error: nodeState.error,
                status: 'FAILED',
            });

            continue;
        }

        if (existingNodeState?.status === 'COMPLETED' || existingNodeState?.status === 'FAILED') {
            if (
                nodeState.durationMillis !== undefined &&
                nodeState.durationMillis !== existingNodeState.durationMillis
            ) {
                setWorkflowTestNodeState(name, {...existingNodeState, durationMillis: nodeState.durationMillis});
            }

            continue;
        }

        setWorkflowTestNodeState(name, nodeState);
    }

    const jobStatus = workflowTestExecution.job?.status;

    if (jobStatus === 'COMPLETED') {
        const currentNodeStates = useWorkflowEditorStore.getState().workflowTestNodeStates;

        const {removeWorkflowTestNodeState} = useWorkflowEditorStore.getState();

        for (const [name, nodeState] of Object.entries(currentNodeStates)) {
            if (nodeState.status !== 'RUNNING') {
                continue;
            }

            if (nodeState.clusterElement) {
                removeWorkflowTestNodeState(name);
            } else {
                setWorkflowTestNodeState(name, {...nodeState, status: 'COMPLETED'});
            }
        }
    } else if (jobStatus === 'FAILED' || jobStatus === 'STOPPED') {
        clearRunningNodeStates();
    }
}

function handleClusterElementCall(data: unknown): string | undefined {
    const payload = parseTaskLifecyclePayload(data);

    if (!payload?.name) {
        return undefined;
    }

    const {setWorkflowTestNodeState, upsertWorkflowTestClusterElementCallRecord, workflowTestNodeStates} =
        useWorkflowEditorStore.getState();

    setWorkflowTestNodeState(payload.name, toClusterElementNodeState(payload, workflowTestNodeStates[payload.name]));

    const record = toClusterElementCallRecord(payload);

    if (record && payload.taskExecutionId) {
        upsertWorkflowTestClusterElementCallRecord(payload.taskExecutionId, record);
    }

    return payload.name;
}

function applyProgressNodeStates(workflowTestExecution: WorkflowTestExecution, progressOwnedNodeNames: Set<string>) {
    const {setWorkflowTestNodeState, workflowTestNodeStates} = useWorkflowEditorStore.getState();

    const progressNodeStates = buildNodeStatesFromTaskExecutions(workflowTestExecution.job?.taskExecutions, true);

    for (const [name, nodeState] of Object.entries(progressNodeStates)) {
        const existingNodeState = workflowTestNodeStates[name];

        if (existingNodeState && !progressOwnedNodeNames.has(name)) {
            continue;
        }

        progressOwnedNodeNames.add(name);

        if (
            existingNodeState?.status === nodeState.status &&
            existingNodeState.durationMillis === nodeState.durationMillis &&
            existingNodeState.error === nodeState.error
        ) {
            continue;
        }

        setWorkflowTestNodeState(name, nodeState);
    }
}

function toClusterElementCallRecord(payload: TaskLifecyclePayloadI): ClusterElementCallRecord | undefined {
    if (!payload.callId || !payload.callStatus) {
        return undefined;
    }

    return ClusterElementCallRecordFromJSON({
        componentName: payload.componentName,
        endDate: payload.callEndDate,
        error: payload.callError,
        id: payload.callId,
        input: payload.input,
        output: payload.output,
        parentId: payload.parentCallId,
        startDate: payload.callStartDate,
        status: payload.callStatus,
        toolName: payload.toolName,
        workflowNodeName: payload.name,
    });
}

function parseTaskLifecyclePayload(data: unknown): TaskLifecyclePayloadI | undefined {
    try {
        const payload = typeof data === 'string' ? JSON.parse(data) : data;

        if (payload && typeof payload === 'object') {
            return payload as TaskLifecyclePayloadI;
        }
    } catch (error) {
        console.error('Failed to parse task lifecycle event:', error);
    }

    return undefined;
}

export interface UseWorkflowTestStreamResultI {
    close: () => void;
    error: string | null;
    getPersistedJobId: () => string | null;
    persistJobId: (jobId: string | null) => void;
    setStreamRequest: (request: SSERequestType) => void;
}

export function useWorkflowTestStream({
    onError,
    onResult,
    onStart,
    onStreamEnd,
    workflowId,
}: UseWorkflowTestStreamProps): UseWorkflowTestStreamResultI {
    const [streamRequest, setStreamRequest] = useState<SSERequestType>(null);

    const progressOwnedNodeNamesRef = useRef(new Set<string>());
    const questionShownRef = useRef(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {resetWorkflowTestNodeStates, setWorkflowIsRunning, setWorkflowTestExecution, setWorkflowTestNodeState} =
        useWorkflowEditorStore(
            useShallow((state) => ({
                resetWorkflowTestNodeStates: state.resetWorkflowTestNodeStates,
                setWorkflowIsRunning: state.setWorkflowIsRunning,
                setWorkflowTestExecution: state.setWorkflowTestExecution,
                setWorkflowTestNodeState: state.setWorkflowTestNodeState,
            }))
        );
    const {appendToLastAssistantMessage, setLastAssistantMessageContent} = useWorkflowTestChatStore(
        useShallow((state) => ({
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            setLastAssistantMessageContent: state.setLastAssistantMessageContent,
        }))
    );

    const {getPersistedJobId, persistJobId} = usePersistJobId(workflowId, currentEnvironmentId);

    const releaseProgressOwnership = (name: string | undefined) => {
        if (name) {
            progressOwnedNodeNamesRef.current.delete(name);
        }
    };

    const {close: closeSSE, error} = useSSE<WorkflowTestExecution>(streamRequest, {
        eventHandlers: {
            ask_user_question: (data) => {
                if (
                    typeof data !== 'object' ||
                    data === null ||
                    !('questions' in data) ||
                    !Array.isArray((data as {questions: unknown}).questions)
                ) {
                    console.error('Received malformed ask_user_question event:', data);

                    setWorkflowIsRunning(false);
                    setStreamRequest(null);

                    if (onError) {
                        onError('The agent asked a question in an unexpected format.');
                    }

                    return;
                }

                const questionEvent = data as AskUserQuestionEventI;

                questionShownRef.current = true;

                setLastAssistantMessageContent(`${formatAskUserQuestionMessage(questionEvent)}\n\n`);
            },
            cluster_element_blocked: (data) => releaseProgressOwnership(handleClusterElementCall(data)),
            cluster_element_completed: (data) => releaseProgressOwnership(handleClusterElementCall(data)),
            cluster_element_denied: (data) => releaseProgressOwnership(handleClusterElementCall(data)),
            cluster_element_failed: (data) => releaseProgressOwnership(handleClusterElementCall(data)),
            cluster_element_redacted: (data) => releaseProgressOwnership(handleClusterElementCall(data)),
            cluster_element_started: (data) => releaseProgressOwnership(handleClusterElementCall(data)),
            cluster_element_withheld: (data) => releaseProgressOwnership(handleClusterElementCall(data)),
            error: (data) => {
                clearRunningNodeStates();
                setWorkflowIsRunning(false);
                setWorkflowTestExecution(undefined);
                setStreamRequest(null);
                persistJobId(null);

                if (onError) {
                    const errorMessage =
                        typeof data === 'string'
                            ? data
                            : data && typeof data === 'object' && 'message' in data
                              ? String((data as {message: string}).message)
                              : undefined;

                    onError(errorMessage);
                }
            },
            progress: (data) => {
                if (!useWorkflowEditorStore.getState().workflowIsRunning) {
                    return;
                }

                try {
                    const progressData = typeof data === 'string' ? JSON.parse(data) : data;

                    const workflowTestExecution = WorkflowTestExecutionFromJSON(progressData);

                    setWorkflowTestExecution(workflowTestExecution);
                    applyProgressNodeStates(workflowTestExecution, progressOwnedNodeNamesRef.current);
                } catch (error) {
                    console.error('Failed to parse workflow test execution progress:', error);
                }
            },
            result: (data) => {
                try {
                    const resultData = typeof data === 'string' ? JSON.parse(data) : data;

                    const workflowTestExecution = WorkflowTestExecutionFromJSON(resultData);

                    const message = workflowTestExecution.job?.outputs?.message ?? '';

                    // Do not overwrite streamed content with empty final text, nor a question asked during the run
                    if (!questionShownRef.current && message && message.trim().length > 0) {
                        setLastAssistantMessageContent(message);
                    }

                    setWorkflowTestExecution(workflowTestExecution);
                    backfillNodeStatesFromResult(workflowTestExecution);

                    if (onResult) {
                        onResult(workflowTestExecution);
                    }
                } catch (error) {
                    console.error('Failed to parse workflow test execution result:', error);

                    if (onError) {
                        onError('Failed to read the workflow test result.');
                    }
                } finally {
                    setWorkflowIsRunning(false);
                    setStreamRequest(null);
                    persistJobId(null);
                }
            },
            start: (data) => {
                try {
                    const startData = typeof data === 'string' ? JSON.parse(data) : (data as {jobId: number});

                    const jobId = String(startData.jobId);

                    resetWorkflowTestNodeStates(workflowId);
                    setWorkflowTestNodeState(getTestRunTriggerName(useWorkflowDataStore.getState().workflow), {
                        status: 'COMPLETED',
                    });

                    progressOwnedNodeNamesRef.current.clear();
                    questionShownRef.current = false;

                    persistJobId(jobId);

                    if (onStart) {
                        onStart(jobId);
                    }
                } catch (parseError) {
                    console.error('Failed to parse workflow start event:', parseError);

                    setWorkflowIsRunning(false);
                    setStreamRequest(null);

                    if (onError) {
                        onError('Failed to start workflow: unable to parse start event');
                    }
                }
            },
            stream: (data) => {
                const chunk = extractStreamChunk(data);

                if (chunk) {
                    appendToLastAssistantMessage(chunk);
                }
            },
            task_completed: (data) => {
                const payload = parseTaskLifecyclePayload(data);

                if (payload?.name) {
                    releaseProgressOwnership(payload.name);

                    const durationMillis = computeDurationMillis(payload.startDate, payload.endDate);

                    setWorkflowTestNodeState(payload.name, {
                        ...(durationMillis != null && {durationMillis}),
                        status: payload.status === 'FAILED' ? 'FAILED' : 'COMPLETED',
                    });
                }
            },
            task_failed: (data) => {
                const payload = parseTaskLifecyclePayload(data);

                if (payload?.name) {
                    releaseProgressOwnership(payload.name);

                    const durationMillis = computeDurationMillis(payload.startDate, payload.endDate);

                    setWorkflowTestNodeState(payload.name, {
                        ...(durationMillis != null && {durationMillis}),
                        error: payload.error,
                        status: 'FAILED',
                    });
                }
            },
            task_started: (data) => {
                const payload = parseTaskLifecyclePayload(data);

                if (payload?.name) {
                    releaseProgressOwnership(payload.name);

                    setWorkflowTestNodeState(payload.name, {status: 'RUNNING'});
                }
            },
        },
        onClose: () => {
            clearRunningNodeStates();
            clearProgressSnapshot();
            setWorkflowIsRunning(false);
            setStreamRequest(null);

            if (onStreamEnd) {
                onStreamEnd();
            }
        },
    });

    const close = useCallback(() => {
        closeSSE();
        clearRunningNodeStates();
        clearProgressSnapshot();
    }, [closeSSE]);

    return {
        close,
        error,
        getPersistedJobId,
        persistJobId,
        setStreamRequest,
    };
}
