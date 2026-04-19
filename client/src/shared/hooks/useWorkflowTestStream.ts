import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {usePersistJobId} from '@/shared/hooks/usePersistJobId';
import {SSERequestType, useSSE} from '@/shared/hooks/useSSE';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {WorkflowTestExecutionFromJSON} from '@/shared/middleware/platform/workflow/test/models/WorkflowTestExecution';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {AskUserQuestionEventI, formatAskUserQuestionMessage} from '@/shared/util/assistant-message-utils';
import {extractStreamChunk} from '@/shared/util/stream-utils';
import {useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

export interface UseWorkflowTestStreamProps {
    workflowId: string;
    onResult?: (execution: WorkflowTestExecution) => void;
    onError?: (errorMessage?: string) => void;
    onStart?: (jobId: string) => void;
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
    workflowId,
}: UseWorkflowTestStreamProps): UseWorkflowTestStreamResultI {
    const [streamRequest, setStreamRequest] = useState<SSERequestType>(null);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {setWorkflowIsRunning, setWorkflowTestExecution} = useWorkflowEditorStore(
        useShallow((state) => ({
            setWorkflowIsRunning: state.setWorkflowIsRunning,
            setWorkflowTestExecution: state.setWorkflowTestExecution,
        }))
    );
    const {appendToLastAssistantMessage, setLastAssistantMessageContent, setResumeUrl} = useWorkflowTestChatStore(
        useShallow((state) => ({
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            setLastAssistantMessageContent: state.setLastAssistantMessageContent,
            setResumeUrl: state.setResumeUrl,
        }))
    );

    const {getPersistedJobId, persistJobId} = usePersistJobId(workflowId, currentEnvironmentId);

    const {close, error} = useSSE<WorkflowTestExecution>(streamRequest, {
        eventHandlers: {
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
        },
    });

    return {
        close,
        error,
        getPersistedJobId,
        persistJobId,
        setStreamRequest,
    };
}
