import {useChatsStore} from '@/pages/automation/chats/stores/useChatsStore';
import {ApprovalResolutionContext, ResumeError} from '@/shared/components/ai-chat/approvalResolutionContext';
import {useSSE} from '@/shared/hooks/useSSE';
import {
    ApprovalRequestEventI,
    AskUserQuestionEventI,
    formatAskUserQuestionMessage,
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
    const [streamRequest, setStreamRequest] = useState<{
        url: string;
        init?: RequestInit;
    } | null>(null);

    const {
        appendToLastAssistantMessage,
        isRunning,
        messages,
        setCurrentStepName,
        setIsRunning,
        setLastAssistantMessageContent,
        setMessage,
        setResumeUrl,
    } = useChatsStore(
        useShallow((state) => ({
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            isRunning: state.isRunning,
            messages: state.messages,
            setCurrentStepName: state.setCurrentStepName,
            setIsRunning: state.setIsRunning,
            setLastAssistantMessageContent: state.setLastAssistantMessageContent,
            setMessage: state.setMessage,
            setResumeUrl: state.setResumeUrl,
        }))
    );

    const handleError = useCallback(() => {
        setIsRunning(false);
        setStreamRequest(null);
    }, [setIsRunning]);

    const handleResult = useCallback(
        (data: unknown) => {
            try {
                const resultData = typeof data === 'string' ? JSON.parse(data) : (data as {message: string});

                const message = resultData?.message ?? '';

                // Do not overwrite streamed content with empty final text
                if (message && message.trim().length > 0) {
                    setLastAssistantMessageContent(message);
                }
            } catch (error) {
                console.error('Failed to parse workflow result:', error);
            } finally {
                setIsRunning(false);
                setStreamRequest(null);
            }
        },
        [setIsRunning, setLastAssistantMessageContent]
    );

    const handleStream = useCallback(
        (data: unknown) => {
            const chunk = extractStreamChunk(data);

            if (chunk) {
                appendToLastAssistantMessage(chunk);
            }
        },
        [appendToLastAssistantMessage]
    );

    const handleTaskStarted = useCallback(
        (data: unknown) => {
            // Enriched coordinator task_started: {taskExecutionId, name, type}. Surface a transient step
            // indicator while the workflow runs; cleared when the run stops (setIsRunning(false)).
            if (typeof data !== 'object' || data === null) {
                return;
            }

            const payload = data as {name?: string; type?: string};
            const stepName = payload.name || payload.type;

            if (stepName) {
                setCurrentStepName(stepName);
            }
        },
        [setCurrentStepName]
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

                return;
            }

            const questionEvent = data as AskUserQuestionEventI;

            setLastAssistantMessageContent(formatAskUserQuestionMessage(questionEvent));
            setResumeUrl(questionEvent.resumeUrl ?? null);
            setIsRunning(false);
            setStreamRequest(null);
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

            // Render an interactive approval card (see ApprovalRequestMessage) that resolves through the
            // job-resume endpoint. Deliberately do NOT set a resume URL for the chat input — typing never
            // resolves an approval; only the card (or the hosted form) does.
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
            setIsRunning(false);
            setStreamRequest(null);
        },
        [setIsRunning, setMessage]
    );

    const eventHandlers = useMemo(
        () => ({
            approval_request: handleApprovalRequest,
            ask_user_question: handleAskUserQuestion,
            error: handleError,
            result: handleResult,
            stream: handleStream,
            task_started: handleTaskStarted,
        }),
        [handleApprovalRequest, handleAskUserQuestion, handleError, handleResult, handleStream, handleTaskStarted]
    );

    const onNew = useCallback(
        async (message: AppendMessage) => {
            if (message.content[0]?.type !== 'text') {
                throw new Error('Only text messages are supported');
            }

            const input = message.content[0].text;
            const currentResumeUrl = useChatsStore.getState().resumeUrl;

            setMessage({attachments: [...(message.attachments ?? [])], content: input, role: 'user'});
            setIsRunning(true);

            if (currentResumeUrl) {
                try {
                    setResumeUrl(null);

                    const response = await fetch(currentResumeUrl, {
                        body: JSON.stringify({message: input}),
                        headers: {'Content-Type': 'application/json'},
                        method: 'POST',
                    });

                    if (!response.ok) {
                        throw new Error(`Resume request failed with status ${response.status}`);
                    }

                    setMessage({content: 'Answer submitted. The workflow will resume.', role: 'assistant'});
                } catch (error) {
                    console.error('Failed to submit answer to resume URL:', error);

                    setMessage({content: 'Failed to submit your answer. Please try again.', role: 'assistant'});
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
                            const result = await res.json();

                            return {
                                error: {
                                    detail: result.detail,
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

    const pendingResumeRef = useRef<{reject: (error: Error) => void; resolve: () => void} | null>(null);

    const {connectionState} = useSSE(streamRequest, {
        eventHandlers,
        onRequestError: (status) => {
            const pending = pendingResumeRef.current;

            pendingResumeRef.current = null;

            pending?.reject(new ResumeError(status));
        },
        onRequestSuccess: () => {
            const pending = pendingResumeRef.current;

            pendingResumeRef.current = null;

            pending?.resolve();
        },
    });

    // Continuation streaming for inline approval cards: resolve through the SSE-negotiated resume endpoint and
    // pipe the resumed run's output back into this conversation via the same event handlers as a normal turn. The
    // returned promise settles from the resume request's HTTP outcome so the card only shows success on a 2xx.
    const resolveApproval = useCallback(
        (resumeId: string, payload: Record<string, unknown>) =>
            new Promise<void>((resolve, reject) => {
                pendingResumeRef.current = {reject, resolve};

                setMessage({content: '', role: 'assistant'});
                setIsRunning(true);
                setStreamRequest({
                    init: {
                        body: JSON.stringify(payload),
                        headers: {'Content-Type': 'application/json'},
                        method: 'POST',
                    },
                    url: `/job/resume/${resumeId}`,
                });
            }),
        [setIsRunning, setMessage]
    );

    const approvalResolution = useMemo(() => ({resolveApproval}), [resolveApproval]);

    useEffect(() => {
        if (connectionState === 'CLOSED' || connectionState === 'ERROR') {
            setIsRunning(false);
        }
    }, [connectionState, setIsRunning]);

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
