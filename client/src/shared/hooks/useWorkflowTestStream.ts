import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {usePersistJobId} from '@/shared/hooks/usePersistJobId';
import {SSERequestType, useSSE} from '@/shared/hooks/useSSE';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {WorkflowTestExecutionFromJSON} from '@/shared/middleware/platform/workflow/test/models/WorkflowTestExecution';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {
    ApprovalRequestEventI,
    AskUserQuestionEventI,
    formatAskUserQuestionMessage,
} from '@/shared/util/assistant-message-utils';
import {extractStreamChunk} from '@/shared/util/stream-utils';
import {useEffect, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

export interface UseWorkflowTestStreamProps {
    workflowId: string;
    /** Fired when the SSE connection closes or errors — streams that end without a `result` event (e.g. an
     * approval-resume continuation) still need the running state released. */
    onClosed?: () => void;
    onResult?: (execution: WorkflowTestExecution) => void;
    onError?: (errorMessage?: string) => void;
    /** Fired with the HTTP status when the request's initial response is non-2xx (or the connection fails) — lets a
     * caller reject a pending approval-resume promise instead of silently ending. */
    onRequestError?: (status: number | null) => void;
    /** Fired once the request's initial response is 2xx and the stream begins. */
    onRequestSuccess?: () => void;
    onStart?: (jobId: string) => void;
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
 * Backfills node states from the final execution result. Task dispatcher executions (condition, loop, ...)
 * complete coordinator-side and never emit worker task lifecycle SSE events, so without this pass their nodes
 * would stay uncolored after a run even though they executed. Streamed states win — only missing (or
 * still-RUNNING) entries are filled in.
 */
function backfillNodeStatesFromResult(workflowTestExecution: WorkflowTestExecution) {
    const {setWorkflowTestNodeState, workflowTestNodeStates} = useWorkflowEditorStore.getState();

    for (const taskExecution of workflowTestExecution.job?.taskExecutions ?? []) {
        const name = taskExecution.workflowTask?.name;

        if (!name) {
            continue;
        }

        const existingStatus = workflowTestNodeStates[name]?.status;

        if (existingStatus === 'COMPLETED' || existingStatus === 'FAILED') {
            continue;
        }

        const status =
            taskExecution.status === 'COMPLETED' ? 'COMPLETED' : taskExecution.status === 'FAILED' ? 'FAILED' : null;

        if (status == null) {
            continue;
        }

        setWorkflowTestNodeState(name, {
            durationMillis: computeDurationMillis(
                taskExecution.startDate?.toISOString(),
                taskExecution.endDate?.toISOString()
            ),
            status,
        });
    }

    // The result payload only carries top-level task executions, so a nested child (condition case, loop
    // iteratee) whose task_completed SSE event was missed has no backfill source. A COMPLETED job proves
    // every started task finished — sweep any node still marked RUNNING to COMPLETED so no spinner outlives
    // the run.
    if (workflowTestExecution.job?.status === 'COMPLETED') {
        const currentNodeStates = useWorkflowEditorStore.getState().workflowTestNodeStates;

        for (const [name, nodeState] of Object.entries(currentNodeStates)) {
            if (nodeState.status === 'RUNNING') {
                setWorkflowTestNodeState(name, {...nodeState, status: 'COMPLETED'});
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
    onClosed,
    onError,
    onRequestError,
    onRequestSuccess,
    onResult,
    onStart,
    workflowId,
}: UseWorkflowTestStreamProps): UseWorkflowTestStreamResultI {
    const [streamRequest, setStreamRequest] = useState<SSERequestType>(null);

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
    const {appendToLastAssistantMessage, setLastAssistantMessageContent, setMessage, setResumeUrl} =
        useWorkflowTestChatStore(
            useShallow((state) => ({
                appendToLastAssistantMessage: state.appendToLastAssistantMessage,
                setLastAssistantMessageContent: state.setLastAssistantMessageContent,
                setMessage: state.setMessage,
                setResumeUrl: state.setResumeUrl,
            }))
        );

    const {getPersistedJobId, persistJobId} = usePersistJobId(workflowId, currentEnvironmentId);

    const {close, connectionState, error} = useSSE<WorkflowTestExecution>(streamRequest, {
        eventHandlers: {
            approval_request: (data) => {
                if (typeof data !== 'object' || data === null || !('resumeId' in data)) {
                    console.error('Received malformed approval_request event:', data);

                    return;
                }

                const approvalEvent = data as ApprovalRequestEventI;

                // Render an interactive approval card (see ApprovalRequestMessage) that resolves through the
                // job-resume endpoint. Deliberately do NOT register a resume URL for the chat input — typing
                // never resolves an approval; only the card (or the hosted form) does.
                setMessage({
                    content: [
                        {
                            data: {
                                expiresAt: approvalEvent.expiresAt,
                                formDescription: approvalEvent.formDescription,
                                formTitle: approvalEvent.formTitle,
                                formUrl: approvalEvent.formUrl,
                                hasInputs: Array.isArray(approvalEvent.inputs) && approvalEvent.inputs.length > 0,
                                kind: 'approval-request',
                                resumeId: approvalEvent.resumeId,
                            },
                            type: 'data-approval-request',
                        },
                    ],
                    role: 'assistant',
                });
                setWorkflowIsRunning(false);
                setStreamRequest(null);
            },
            ask_user_question: (data) => {
                if (
                    typeof data !== 'object' ||
                    data === null ||
                    !('questions' in data) ||
                    !Array.isArray((data as {questions: unknown}).questions)
                ) {
                    console.error('Received malformed ask_user_question event:', data);

                    return;
                }

                const questionEvent = data as AskUserQuestionEventI;

                setLastAssistantMessageContent(formatAskUserQuestionMessage(questionEvent));
                setResumeUrl(questionEvent.resumeUrl ?? null);
                setWorkflowIsRunning(false);
                setStreamRequest(null);
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
            result: (data) => {
                try {
                    const resultData = typeof data === 'string' ? JSON.parse(data) : data;

                    const workflowTestExecution = WorkflowTestExecutionFromJSON(resultData);

                    const message = workflowTestExecution.job?.outputs?.message ?? '';

                    // Do not overwrite streamed content with empty final text
                    if (message && message.trim().length > 0) {
                        setLastAssistantMessageContent(message);
                    }

                    setWorkflowTestExecution(workflowTestExecution);
                    backfillNodeStatesFromResult(workflowTestExecution);

                    if (onResult) {
                        onResult(workflowTestExecution);
                    }
                } catch (error) {
                    console.error('Failed to parse workflow test execution result:', error);
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
                    setWorkflowTestNodeState(payload.name, {error: payload.error, status: 'FAILED'});
                }
            },
            task_started: (data) => {
                const payload = parseTaskLifecyclePayload(data);

                if (payload?.name) {
                    setWorkflowTestNodeState(payload.name, {status: 'RUNNING'});
                }
            },
        },
        onRequestError,
        onRequestSuccess,
    });

    useEffect(() => {
        if (connectionState === 'CLOSED' || connectionState === 'ERROR') {
            setWorkflowIsRunning(false);

            if (onClosed) {
                onClosed();
            }
        }
    }, [connectionState, onClosed, setWorkflowIsRunning]);

    return {
        close,
        error,
        getPersistedJobId,
        persistJobId,
        setStreamRequest,
    };
}
