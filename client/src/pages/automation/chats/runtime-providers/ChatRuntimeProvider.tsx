import {useChatsStore} from '@/pages/automation/chats/stores/useChatsStore';
import {useSSE} from '@/shared/hooks/useSSE';
import {AskUserQuestionEventI, formatAskUserQuestionMessage} from '@/shared/util/assistant-message-utils';
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
import {ReactNode, memo, useCallback, useEffect, useMemo, useState} from 'react';
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
        setIsRunning,
        setLastAssistantMessageContent,
        setMessage,
        setResumeUrl,
    } = useChatsStore(
        useShallow((state) => ({
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            isRunning: state.isRunning,
            messages: state.messages,
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

    const eventHandlers = useMemo(
        () => ({
            ask_user_question: handleAskUserQuestion,
            error: handleError,
            result: handleResult,
            stream: handleStream,
        }),
        [handleAskUserQuestion, handleError, handleResult, handleStream]
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

    const {connectionState} = useSSE(streamRequest, {eventHandlers});

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

    return <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>;
});
