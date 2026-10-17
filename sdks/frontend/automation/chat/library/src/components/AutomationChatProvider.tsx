import {
    AppendMessage,
    AssistantRuntimeProvider,
    CompositeAttachmentAdapter,
    SimpleImageAttachmentAdapter,
    SimpleTextAttachmentAdapter,
    Suggestions,
    ThreadMessageLike,
    useAui,
    useExternalStoreRuntime,
} from '@assistant-ui/react';
import {ReactNode, memo, useCallback, useEffect, useMemo, useState} from 'react';
import {useShallow} from 'zustand/shallow';

import {useChatStore} from '@/stores/useChatStore';
import type {PendingApprovalI} from '@/stores/useChatStore';
import {ApprovalResolutionContext} from '@/components/approvalResolutionContext';
import {useSSE} from '@/hooks/useSSE';
import {extractStreamChunk} from '@/utils/stream-utils';
import {drainSseResponse} from '@/utils/sse-parser';
import {
    ApprovalRequestEventI,
    AskUserQuestionEventI,
    formatApprovalRequest,
    formatAskUserQuestion,
    getApprovalResumeUrl,
    isAcceptedResumeResponse,
    isEventStreamResponse,
} from '@/utils/approval-utils';
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

    const {appendToLastAssistantMessage, messages, setLastAssistantMessageContent, setMessage, setResumeUrl} =
        useChatStore(
            useShallow((state) => ({
                appendToLastAssistantMessage: state.appendToLastAssistantMessage,
                messages: state.messages,
                setLastAssistantMessageContent: state.setLastAssistantMessageContent,
                setMessage: state.setMessage,
                setResumeUrl: state.setResumeUrl,
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

    const handleAskUserQuestion = useCallback(
        (data: unknown) => {
            if (!data || typeof data !== 'object') {
                return;
            }

            const event = data as AskUserQuestionEventI;
            const formatted = formatAskUserQuestion(event);

            if (formatted) {
                setLastAssistantMessageContent(formatted);
            }

            setResumeUrl(event.resumeUrl ?? null);
            setIsRunning(false);
            setStreamRequest(null);
        },
        [setLastAssistantMessageContent, setResumeUrl]
    );

    const handleApprovalRequest = useCallback(
        (data: unknown) => {
            if (!data || typeof data !== 'object' || !('resumeId' in data)) {
                return;
            }

            const event = data as ApprovalRequestEventI;

            const hasInputs = Array.isArray(event.inputs) && event.inputs.length > 0;
            const approvalResumeUrl = getApprovalResumeUrl(event, webhookUrl);

            if (!hasInputs && approvalResumeUrl) {
                useChatStore.getState().setPendingApproval({
                    expiresAt: event.expiresAt,
                    formDescription: event.formDescription,
                    formTitle: event.formTitle,
                    resumeUrl: approvalResumeUrl,
                });
            } else {
                setMessage({content: formatApprovalRequest(event), role: 'assistant'});
            }

            setIsRunning(false);
            setStreamRequest(null);
        },
        [setMessage, webhookUrl]
    );

    const eventHandlers = useMemo(
        () => ({
            approval_request: handleApprovalRequest,
            ask_user_question: handleAskUserQuestion,
            error: handleError,
            result: handleResult,
            stream: handleStream,
            message: handleResult, // Handle default SSE 'message' events as results
        }),
        [handleApprovalRequest, handleAskUserQuestion, handleError, handleResult, handleStream]
    );

    const resolveApproval = useCallback(
        async (pendingApproval: PendingApprovalI, payload: Record<string, unknown>) => {
            const response = await fetch(pendingApproval.resumeUrl, {
                body: JSON.stringify(payload),
                headers: {Accept: 'text/event-stream', 'Content-Type': 'application/json'},
                method: 'POST',
            });

            if (!isAcceptedResumeResponse(response)) {
                throw new Error(`Resume request was not accepted (status ${response.status})`);
            }

            const resolvedTitle = pendingApproval.formTitle || 'Approval requested';

            useChatStore.getState().setPendingApproval(null);
            setMessage({
                content: `**${resolvedTitle}** — ${payload.approved ? 'approved' : 'discarded'}.`,
                role: 'assistant',
            });

            if (isEventStreamResponse(response)) {
                setIsRunning(true);
                setMessage({content: '', role: 'assistant'});

                void drainSseResponse(response, eventHandlers)
                    .catch((drainError) => console.error('Failed to stream the resumed run:', drainError))
                    .finally(() => setIsRunning(false));
            }
        },
        [eventHandlers, setMessage]
    );

    const approvalResolution = useMemo(() => ({resolveApproval}), [resolveApproval]);

    const onNew = useCallback(
        async (message: AppendMessage) => {
            if (message.content[0]?.type !== 'text') {
                throw new Error('Only text messages are supported');
            }

            const input = message.content[0].text;

            const currentResumeUrl = useChatStore.getState().resumeUrl;

            if (currentResumeUrl) {
                useChatStore.getState().setResumeUrl(null);

                setMessage({attachments: [...(message.attachments ?? [])], content: input, role: 'user'});
                setIsRunning(true);

                const headers: Record<string, string> = sseEnabled
                    ? {Accept: 'text/event-stream', 'Content-Type': 'application/json'}
                    : {'Content-Type': 'application/json'};

                try {
                    const response = await fetch(currentResumeUrl, {
                        body: JSON.stringify({message: input}),
                        headers,
                        method: 'POST',
                    });

                    if (!isAcceptedResumeResponse(response)) {
                        throw new Error(`Resume request was not accepted (status ${response.status})`);
                    }

                    if (isEventStreamResponse(response)) {
                        setMessage({content: '', role: 'assistant'});

                        await drainSseResponse(response, eventHandlers);
                    } else {
                        const result = (await response.json().catch(() => null)) as {message?: string} | null;
                        const responseText = result?.message ?? 'Answer submitted. The workflow will resume.';

                        setMessage({content: responseText, role: 'assistant'});
                    }
                } catch (error) {
                    console.error('Failed to submit answer to resume URL:', error);

                    setMessage({
                        content: 'Failed to submit your answer. Please try again.',
                        role: 'assistant',
                    });
                } finally {
                    setIsRunning(false);
                }

                return;
            }

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
        [eventHandlers, setMessage, sseEnabled, webhookUrl]
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

    const aui = useAui({suggestions: Suggestions(suggestions ?? [])}, {parent: null});

    const {connectionState} = useSSE(sseEnabled ? streamRequest : null, {eventHandlers});

    useEffect(() => {
        if (sseEnabled && (connectionState === 'CLOSED' || connectionState === 'ERROR')) {
            setIsRunning(false);
        }
    }, [connectionState, sseEnabled]);

    return (
        <AutomationChatContext.Provider value={contextValue}>
            <ApprovalResolutionContext.Provider value={approvalResolution}>
                <AssistantRuntimeProvider aui={aui} runtime={runtime}>
                    {children}
                </AssistantRuntimeProvider>
            </ApprovalResolutionContext.Provider>
        </AutomationChatContext.Provider>
    );
});
