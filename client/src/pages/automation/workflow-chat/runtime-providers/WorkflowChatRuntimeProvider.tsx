import {useWorkflowChatStore} from '@/pages/automation/workflow-chat/stores/useWorkflowChatStore';
import {useSSE} from '@/shared/hooks/useSSE';
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

export const WorkflowChatRuntimeProvider = memo(function WorkflowChatRuntimeProvider({
    children,
    environment,
    sseStream,
    workflowExecutionId,
}: Readonly<{
    environment: string;
    workflowExecutionId: string;
    sseStream?: boolean;
    children: ReactNode;
}>) {
    const [isRunning, setIsRunning] = useState(false);
    const [streamRequest, setStreamRequest] = useState<{
        url: string;
        init?: RequestInit;
    } | null>(null);

    const {appendToLastAssistantMessage, messages, setLastAssistantMessageContent, setMessage} = useWorkflowChatStore(
        useShallow((state) => ({
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            messages: state.messages,
            setLastAssistantMessageContent: state.setLastAssistantMessageContent,
            setMessage: state.setMessage,
        }))
    );

    const handleError = useCallback(() => {
        setIsRunning(false);
        setStreamRequest(null);
    }, []);

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
        [setLastAssistantMessageContent]
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

    const eventHandlers = useMemo(
        () => ({
            error: handleError,
            result: handleResult,
            stream: handleStream,
        }),
        [handleError, handleResult, handleStream]
    );

    const onNew = useCallback(
        async (message: AppendMessage) => {
            if (message.content[0]?.type !== 'text') {
                throw new Error('Only text messages are supported');
            }

            const input = message.content[0].text;

            setMessage({attachments: [...(message.attachments ?? [])], content: input, role: 'user'});
            setIsRunning(true);

            const formData = new FormData();

            const conversationId = useWorkflowChatStore.getState().conversationId;

            formData.append('conversationId', conversationId ?? '');
            formData.append('message', input ?? '');

            for (const attachment of message.attachments ?? []) {
                if (attachment.file) {
                    formData.append('attachments', attachment.file, attachment.name);
                }
            }

            if (sseStream) {
                setMessage({content: '', role: 'assistant'});

                setStreamRequest({
                    init: {
                        body: formData,
                        headers: {
                            'X-Environment': environment,
                        },
                        method: 'POST',
                    },
                    url: '/webhooks/' + workflowExecutionId + '/sse',
                });
            } else {
                const result = await fetch('/webhooks/' + workflowExecutionId, {
                    body: formData,
                    headers: {
                        'X-Environment': environment,
                    },
                    method: 'POST',
                    // if the user hits the "cancel" button or escape keyboard key, cancel the request
                    // signal: abortSignal,
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

                setIsRunning(false);
            }
        },
        [environment, setMessage, sseStream, workflowExecutionId]
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
    }, [connectionState]);

    return <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>;
});
