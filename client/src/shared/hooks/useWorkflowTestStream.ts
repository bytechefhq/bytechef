import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import buildNodeStatesFromTaskExecutions from '@/pages/platform/workflow-editor/utils/buildNodeStatesFromTaskExecutions';
import {usePersistJobId} from '@/shared/hooks/usePersistJobId';
import {SSERequestType, useSSE} from '@/shared/hooks/useSSE';
import {JobStatusEnum, WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
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
    endDate?: string;
    error?: string;
    name?: string;
    startDate?: string;
    status?: string;
    taskExecutionId?: string;
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

/**
 * Drops a `progress` snapshot left behind by a run that ends without a `result` event. Such a snapshot still reads as
 * running, so keeping it would show a stopped run as in flight; a finished result is left alone.
 */
function clearProgressSnapshot() {
    const {setWorkflowTestExecution, workflowTestExecution} = useWorkflowEditorStore.getState();

    const jobStatus = workflowTestExecution?.job?.status;

    if (jobStatus === JobStatusEnum.Created || jobStatus === JobStatusEnum.Started) {
        setWorkflowTestExecution(undefined);
    }
}

/**
 * Backfills node states from the final execution result. A task dispatcher (condition, loop, ...) whose body ran
 * completes coordinator-side and never emits a task lifecycle SSE event, so without this pass its node -- and every
 * edge touching it -- would stay uncolored after a run even though it executed. The whole execution tree is walked,
 * not just the top level: a condition inside a loop iteration or another condition's branch is exactly such a
 * dispatcher. Streamed states win — only missing (or still-RUNNING) entries are filled in.
 */
function backfillNodeStatesFromResult(workflowTestExecution: WorkflowTestExecution) {
    const {setWorkflowTestNodeState, workflowTestNodeStates} = useWorkflowEditorStore.getState();

    const resultNodeStates = buildNodeStatesFromTaskExecutions(workflowTestExecution.job?.taskExecutions, false);

    for (const [name, nodeState] of Object.entries(resultNodeStates)) {
        const existingStatus = workflowTestNodeStates[name]?.status;

        if (existingStatus === 'COMPLETED' || existingStatus === 'FAILED') {
            continue;
        }

        setWorkflowTestNodeState(name, nodeState);
    }

    // A nested child that neither streamed a task_completed event nor made it into the result tree has no
    // backfill source. A COMPLETED job proves every started task finished — sweep any node still marked RUNNING
    // to COMPLETED so no spinner outlives the run.
    const jobStatus = workflowTestExecution.job?.status;

    if (jobStatus === 'COMPLETED') {
        const currentNodeStates = useWorkflowEditorStore.getState().workflowTestNodeStates;

        for (const [name, nodeState] of Object.entries(currentNodeStates)) {
            if (nodeState.status === 'RUNNING') {
                setWorkflowTestNodeState(name, {...nodeState, status: 'COMPLETED'});
            }
        }
    } else if (jobStatus === 'FAILED' || jobStatus === 'STOPPED') {
        const {removeWorkflowTestNodeState, workflowTestNodeStates: currentNodeStates} =
            useWorkflowEditorStore.getState();

        for (const [name, nodeState] of Object.entries(currentNodeStates)) {
            if (nodeState.status === 'RUNNING') {
                removeWorkflowTestNodeState(name);
            }
        }
    }
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
            error: (data) => {
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
                // A snapshot that lands after the run was stopped (or already finished) must not repaint the panel
                // as running.
                if (!useWorkflowEditorStore.getState().workflowIsRunning) {
                    return;
                }

                try {
                    const progressData = typeof data === 'string' ? JSON.parse(data) : data;

                    setWorkflowTestExecution(WorkflowTestExecutionFromJSON(progressData));
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
                    setWorkflowTestNodeState(payload.name, {status: 'RUNNING'});
                }
            },
        },
        onClose: () => {
            setWorkflowIsRunning(false);
            setStreamRequest(null);

            if (onStreamEnd) {
                onStreamEnd();
            }
        },
    });

    const close = useCallback(() => {
        closeSSE();
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
