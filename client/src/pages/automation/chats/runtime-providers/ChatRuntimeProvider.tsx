import {useChatsStore} from '@/pages/automation/chats/stores/useChatsStore';
import {ApprovalResolutionContext, ResumeError} from '@/shared/components/ai-chat/approvalResolutionContext';
import {SSERequestType, useSSE} from '@/shared/hooks/useSSE';
import {
    ApprovalRequestEventI,
    AskUserQuestionEventI,
    formatAskUserQuestionMessage,
    getResumeFailure,
    getResumeStreamRequest,
    hasLastAssistantMessageText,
} from '@/shared/util/assistant-message-utils';
import {extractStreamChunk} from '@/shared/util/stream-utils';
import {
    AppendMessage,
    AssistantRuntimeProvider,
    CompositeAttachmentAdapter,
    SimpleImageAttachmentAdapter,
    SimpleTextAttachmentAdapter,
    ThreadMessageLike,
    useExternalStoreRuntime,
} from '@assistant-ui/react';
import {ReactNode, memo, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => {
    return message;
};

const SUSPENDING_TASK_IN_SYNC_EXECUTION_ERROR_KEY = 100;

const SUSPENDING_TASK_IN_SYNC_EXECUTION_MESSAGE =
    "This chat can't pause the run for an approval or wait step. A chat can only wait on one when it streams " +
    'its reply, so add a step that streams, such as an AI Agent, after it.';

interface ProblemDetailI {
    detail?: string;
    entityClass?: string;
    errorKey?: number;
}

const isSuspendingTaskInSyncExecutionError = (problemDetail: ProblemDetailI) =>
    problemDetail.entityClass === 'WebhookErrorType' &&
    problemDetail.errorKey === SUSPENDING_TASK_IN_SYNC_EXECUTION_ERROR_KEY;

const attachmentAdapter = new CompositeAttachmentAdapter([
    new SimpleImageAttachmentAdapter(),
    new SimpleTextAttachmentAdapter(),
]);

export const ChatRuntimeProvider = memo(function ChatRuntimeProvider({
    children,
    environmentName,
    sseStreamResponse,
    workflowExecutionId,
}: Readonly<{
    environmentName: string;
    workflowExecutionId: string;
    sseStreamResponse?: boolean;
    children: ReactNode;
}>) {
    const [streamRequest, setStreamRequest] = useState<SSERequestType>(null);

    const approvalCardShownRef = useRef(false);
    const approvalStreamRef = useRef(false);
    const awaitingStreamEventRef = useRef(false);
    const pendingResumeRef = useRef<{
        reject: (error: Error) => void;
        request: SSERequestType;
        resolve: () => void;
    } | null>(null);
    const pendingResumeUrlRef = useRef<string | null>(null);
    const questionShownRef = useRef(false);

    const {
        appendToLastAssistantMessage,
        isRunning,
        messages,
        setIsRunning,
        setLastAssistantMessageContent,
        setLastEmptyAssistantMessage,
        setMessage,
        setResumeUrl,
    } = useChatsStore(
        useShallow((state) => ({
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            isRunning: state.isRunning,
            messages: state.messages,
            setIsRunning: state.setIsRunning,
            setLastAssistantMessageContent: state.setLastAssistantMessageContent,
            setLastEmptyAssistantMessage: state.setLastEmptyAssistantMessage,
            setMessage: state.setMessage,
            setResumeUrl: state.setResumeUrl,
        }))
    );

    const handleError = useCallback(
        (data: unknown) => {
            const errorMessage =
                typeof data === 'string' && data.trim().length > 0
                    ? data
                    : data && typeof data === 'object' && 'message' in data
                      ? String((data as {message: unknown}).message)
                      : 'An unexpected error occurred';

            awaitingStreamEventRef.current = false;

            if (questionShownRef.current) {
                questionShownRef.current = false;

                appendToLastAssistantMessage(`\n\n${errorMessage}`);
                setResumeUrl(null);
            } else {
                setLastAssistantMessageContent(errorMessage);
            }

            setIsRunning(false);
            setStreamRequest(null);
        },
        [appendToLastAssistantMessage, setIsRunning, setLastAssistantMessageContent, setResumeUrl]
    );

    const handleResult = useCallback(
        (data: unknown) => {
            awaitingStreamEventRef.current = false;

            try {
                const resultData = typeof data === 'string' ? JSON.parse(data) : (data as {message: string});

                const message = resultData?.message ?? '';

                // Do not overwrite streamed content with empty final text, nor a question asked during the run
                if (!questionShownRef.current && message && message.trim().length > 0) {
                    setLastAssistantMessageContent(message);
                }
            } catch (error) {
                console.error('Failed to parse workflow result:', error);

                if (!questionShownRef.current && !hasLastAssistantMessageText(useChatsStore.getState().messages)) {
                    setLastAssistantMessageContent('Failed to read the workflow result.');
                }
            } finally {
                if (questionShownRef.current) {
                    questionShownRef.current = false;

                    setResumeUrl(null);
                }

                setIsRunning(false);
                setStreamRequest(null);
            }
        },
        [setIsRunning, setLastAssistantMessageContent, setResumeUrl]
    );

    const handleStream = useCallback(
        (data: unknown) => {
            const chunk = extractStreamChunk(data);

            if (chunk) {
                awaitingStreamEventRef.current = false;

                appendToLastAssistantMessage(chunk);
            }
        },
        [appendToLastAssistantMessage]
    );

    const handleAskUserQuestion = useCallback(
        (data: unknown) => {
            if (
                typeof data !== 'object' ||
                data === null ||
                !('questions' in data) ||
                !Array.isArray((data as {questions: unknown}).questions)
            ) {
                console.error('Received malformed ask_user_question event:', data);

                awaitingStreamEventRef.current = false;

                setLastAssistantMessageContent('The agent asked a question in an unexpected format.');
                setIsRunning(false);
                setStreamRequest(null);

                return;
            }

            const questionEvent = data as AskUserQuestionEventI;

            awaitingStreamEventRef.current = false;
            questionShownRef.current = true;

            setLastAssistantMessageContent(formatAskUserQuestionMessage(questionEvent));
            setResumeUrl(questionEvent.resumeUrl ?? null);
            setIsRunning(false);
        },
        [setIsRunning, setLastAssistantMessageContent, setResumeUrl]
    );

    const handleApprovalRequest = useCallback(
        (data: unknown) => {
            if (typeof data !== 'object' || data === null || !('resumeId' in data)) {
                console.error('Received malformed approval_request event:', data);

                return;
            }

            const approvalEvent = data as ApprovalRequestEventI;

            approvalCardShownRef.current = true;
            awaitingStreamEventRef.current = false;

            setLastEmptyAssistantMessage({
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
            setIsRunning(false);
            setStreamRequest(null);
        },
        [setIsRunning, setLastEmptyAssistantMessage]
    );

    const handleSuspended = useCallback(() => {
        awaitingStreamEventRef.current = false;

        if (!approvalCardShownRef.current && !hasLastAssistantMessageText(useChatsStore.getState().messages)) {
            setLastAssistantMessageContent('The workflow is waiting for a response before it can continue.');
        }

        setIsRunning(false);
        setStreamRequest(null);
    }, [setIsRunning, setLastAssistantMessageContent]);

    const eventHandlers = useMemo(
        () => ({
            approval_request: handleApprovalRequest,
            ask_user_question: handleAskUserQuestion,
            error: handleError,
            result: handleResult,
            stream: handleStream,
            suspended: handleSuspended,
        }),
        [handleApprovalRequest, handleAskUserQuestion, handleError, handleResult, handleStream, handleSuspended]
    );

    const onNew = useCallback(
        async (message: AppendMessage) => {
            if (message.content[0]?.type !== 'text') {
                throw new Error('Only text messages are supported');
            }

            const input = message.content[0].text;
            const currentResumeUrl = useChatsStore.getState().resumeUrl;

            approvalCardShownRef.current = false;
            approvalStreamRef.current = false;
            questionShownRef.current = false;

            setMessage({attachments: [...(message.attachments ?? [])], content: input, role: 'user'});
            setIsRunning(true);

            if (currentResumeUrl && sseStreamResponse) {
                awaitingStreamEventRef.current = true;
                pendingResumeUrlRef.current = currentResumeUrl;

                setResumeUrl(null);
                setMessage({content: '', role: 'assistant'});
                setStreamRequest(getResumeStreamRequest(currentResumeUrl, input));

                return;
            }

            if (currentResumeUrl) {
                const showResumeFailure = (status: number | null) => {
                    const resumeFailure = getResumeFailure(status);

                    if (resumeFailure.retryable) {
                        setResumeUrl(currentResumeUrl);
                    }

                    setMessage({content: resumeFailure.message, role: 'assistant'});
                };

                try {
                    setResumeUrl(null);

                    const response = await fetch(currentResumeUrl, {
                        body: JSON.stringify({message: input}),
                        headers: {'Content-Type': 'application/json'},
                        method: 'POST',
                    });

                    if (response.ok) {
                        setMessage({content: 'Answer submitted. The workflow will resume.', role: 'assistant'});
                    } else {
                        showResumeFailure(response.status);
                    }
                } catch (error) {
                    console.error('Failed to submit answer to resume URL:', error);

                    showResumeFailure(null);
                } finally {
                    setIsRunning(false);
                }

                return;
            }

            const formData = new FormData();

            const conversationId = useChatsStore.getState().conversationId;

            formData.append('conversationId', conversationId ?? '');
            formData.append('message', input ?? '');

            for (const attachment of message.attachments ?? []) {
                if (attachment.file) {
                    formData.append('attachments', attachment.file, attachment.name);
                }
            }

            if (sseStreamResponse) {
                awaitingStreamEventRef.current = true;
                pendingResumeUrlRef.current = null;

                setMessage({content: '', role: 'assistant'});

                setStreamRequest({
                    init: {
                        body: formData,
                        headers: {
                            'X-Environment': environmentName,
                        },
                        method: 'POST',
                    },
                    url: '/webhooks/' + workflowExecutionId + '/sse',
                });
            } else {
                try {
                    const result = await fetch('/webhooks/' + workflowExecutionId, {
                        body: formData,
                        headers: {
                            'X-Environment': environmentName,
                        },
                        method: 'POST',
                    }).then(async (res) => {
                        if (res.status >= 400) {
                            const problemDetail: ProblemDetailI = await res.json();

                            if (isSuspendingTaskInSyncExecutionError(problemDetail)) {
                                return {error: {message: SUSPENDING_TASK_IN_SYNC_EXECUTION_MESSAGE}};
                            }

                            return {
                                error: {
                                    detail: problemDetail.detail,
                                    message: 'An error occurred',
                                },
                            };
                        } else {
                            return res.json();
                        }
                    });

                    if (result?.questions && Array.isArray(result.questions)) {
                        const questionEvent = result as AskUserQuestionEventI;

                        setMessage({
                            content: formatAskUserQuestionMessage(questionEvent),
                            role: 'assistant',
                        });
                        setResumeUrl(questionEvent.resumeUrl ?? null);
                        setIsRunning(false);

                        return;
                    }

                    const content =
                        result?.message ??
                        (result?.error
                            ? (result.error.message ?? 'An error occurred') +
                              (result.error.detail ? '\n' + result.error.detail : '')
                            : 'An unknown error occurred');

                    setMessage({
                        content,
                        role: 'assistant',
                    });
                } catch (error) {
                    console.error('Failed to send chat message:', error);

                    setMessage({
                        content: 'An error occurred while sending the message.',
                        role: 'assistant',
                    });
                } finally {
                    setIsRunning(false);
                }
            }
        },
        [environmentName, setIsRunning, setMessage, setResumeUrl, sseStreamResponse, workflowExecutionId]
    );

    const runtime = useExternalStoreRuntime(
        useMemo(
            () => ({
                adapters: {
                    attachments: attachmentAdapter,
                },
                convertMessage,
                isRunning,
                messages,
                onNew,
            }),
            [isRunning, messages, onNew]
        )
    );

    const takePendingResume = (request: SSERequestType) => {
        const pending = pendingResumeRef.current;

        if (!pending || pending.request !== request) {
            return null;
        }

        pendingResumeRef.current = null;

        return pending;
    };

    const {connectionState, error, errorStatus} = useSSE(streamRequest, {
        eventHandlers,
        onRequestAbort: (request) => {
            takePendingResume(request)?.reject(new ResumeError(null));
        },
        onRequestError: (status, request) => {
            takePendingResume(request)?.reject(new ResumeError(status));
        },
        onRequestSuccess: (request) => {
            const pending = takePendingResume(request);

            if (pending) {
                approvalCardShownRef.current = false;
                approvalStreamRef.current = false;

                setMessage({content: '', role: 'assistant'});

                pending.resolve();
            }
        },
    });

    const resolveApproval = useCallback(
        (resumeId: string, payload: Record<string, unknown>) => {
            if (pendingResumeRef.current) {
                return Promise.reject(
                    new Error('Another decision is being submitted. Wait for it to finish, then try again.')
                );
            }

            return new Promise<void>((resolve, reject) => {
                const request: SSERequestType = {
                    init: {
                        body: JSON.stringify(payload),
                        headers: {'Content-Type': 'application/json'},
                        method: 'POST',
                    },
                    url: `/job/resume/${resumeId}`,
                };

                pendingResumeRef.current = {reject, request, resolve};
                approvalStreamRef.current = true;
                awaitingStreamEventRef.current = false;
                pendingResumeUrlRef.current = null;

                setIsRunning(true);
                setStreamRequest(request);
            });
        },
        [setIsRunning]
    );

    const approvalResolution = useMemo(() => ({resolveApproval}), [resolveApproval]);

    useEffect(() => {
        if (connectionState === 'CONNECTED') {
            pendingResumeUrlRef.current = null;
        }

        if (connectionState === 'ERROR' && pendingResumeUrlRef.current) {
            const resumeFailure = getResumeFailure(errorStatus);

            setLastAssistantMessageContent(resumeFailure.message);

            if (resumeFailure.retryable) {
                setResumeUrl(pendingResumeUrlRef.current);
            }

            pendingResumeUrlRef.current = null;
        } else if (connectionState === 'ERROR' && !approvalStreamRef.current) {
            const errorMessage = `The request failed: ${error || 'Connection error occurred'}`;

            if (hasLastAssistantMessageText(useChatsStore.getState().messages)) {
                appendToLastAssistantMessage(`\n\n${errorMessage}`);
            } else {
                setLastAssistantMessageContent(errorMessage);
            }
        }

        if (connectionState === 'CLOSED' && awaitingStreamEventRef.current) {
            if (!hasLastAssistantMessageText(useChatsStore.getState().messages)) {
                setLastAssistantMessageContent('The response ended unexpectedly.');
            }
        }

        if (connectionState === 'CLOSED' || connectionState === 'ERROR') {
            awaitingStreamEventRef.current = false;

            setIsRunning(false);
        }
    }, [
        appendToLastAssistantMessage,
        connectionState,
        error,
        errorStatus,
        setIsRunning,
        setLastAssistantMessageContent,
        setResumeUrl,
    ]);

    // Reset isRunning on unmount to prevent permanently blocked navigation
    useEffect(() => {
        return () => {
            setIsRunning(false);
        };
    }, [setIsRunning]);

    return (
        <ApprovalResolutionContext.Provider value={approvalResolution}>
            <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>
        </ApprovalResolutionContext.Provider>
    );
});
