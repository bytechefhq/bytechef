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
import {useShallow} from 'zustand/shallow';

import {useChatStore} from '@/stores/useChatStore';
import {useSSE} from '@/hooks/useSSE';
import {extractStreamChunk} from '@/utils/stream-utils';
import {AutomationChatContext} from '@/hooks/useAutomationChatConfig';
import type {AutomationChatConfig} from '@/types';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => {
    return message;
};

const attachmentAdapter = new CompositeAttachmentAdapter([
    new SimpleImageAttachmentAdapter(),
    new SimpleTextAttachmentAdapter(),
]);

type AutomationChatProviderProps = {
    children: ReactNode;
    config: AutomationChatConfig;
};

export const AutomationChatProvider = memo(function AutomationChatProvider({
    children,
    config,
}: AutomationChatProviderProps) {
    const {webhookUrl, title = 'Hello there!', description = 'How can I help you today?', suggestions} = config;

    const contextValue = useMemo(
        () => ({
            title,
            description,
            suggestions,
        }),
        [title, description, suggestions]
    );

    // Automatically detect SSE mode based on URL ending
    const sseEnabled = webhookUrl.endsWith('/sse');

    const [isRunning, setIsRunning] = useState(false);
    const [streamRequest, setStreamRequest] = useState<{
        url: string;
        init?: RequestInit;
    } | null>(null);

    const {appendToLastAssistantMessage, messages, setLastAssistantMessageContent, setMessage} = useChatStore(
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
            console.log('[AutomationChatProvider] handleResult called with:', data);

            // Skip empty data - don't close stream yet
            if (!data || (typeof data === 'string' && data.trim().length === 0)) {
                console.log('[AutomationChatProvider] Skipping empty data');
                return;
            }

            try {
                const resultData = typeof data === 'string' ? JSON.parse(data) : (data as {message: string});

                const message = resultData?.message ?? '';

                console.log('[AutomationChatProvider] Result message:', message);

                // Do not overwrite streamed content with empty final text
                if (message && message.trim().length > 0) {
                    setLastAssistantMessageContent(message);
                }

                // Only close stream after successfully processing non-empty data
                setIsRunning(false);
                setStreamRequest(null);
            } catch (error) {
                console.error('Failed to parse workflow result:', error);
                setIsRunning(false);
                setStreamRequest(null);
            }
        },
        [setLastAssistantMessageContent]
    );

    const handleStream = useCallback(
        (data: unknown) => {
            console.log('[AutomationChatProvider] handleStream called with:', data);
            const chunk = extractStreamChunk(data);

            console.log('[AutomationChatProvider] Extracted chunk:', chunk);

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
            message: handleResult, // Handle default SSE 'message' events as results
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

            const conversationId = useChatStore.getState().conversationId;

            formData.append('conversationId', conversationId ?? '');
            formData.append('message', input ?? '');

            for (const attachment of message.attachments ?? []) {
                if (attachment.file) {
                    formData.append('attachments', attachment.file, attachment.name);
                }
            }

            if (sseEnabled) {
                setMessage({content: '', role: 'assistant'});

                setStreamRequest({
                    init: {
                        body: formData,
                        method: 'POST',
                    },
                    url: webhookUrl,
                });
            } else {
                // Non-SSE mode: regular HTTP fetch
                const result = await fetch(webhookUrl, {
                    body: formData,
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
        [setMessage, sseEnabled, webhookUrl]
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

    const {connectionState} = useSSE(sseEnabled ? streamRequest : null, {eventHandlers});

    useEffect(() => {
        if (sseEnabled && (connectionState === 'CLOSED' || connectionState === 'ERROR')) {
            setIsRunning(false);
        }
    }, [connectionState, sseEnabled]);

    return (
        <AutomationChatContext.Provider value={contextValue}>
            <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>
        </AutomationChatContext.Provider>
    );
});
