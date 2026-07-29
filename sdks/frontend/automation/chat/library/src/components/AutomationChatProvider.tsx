import {
    AppendMessage,
    AssistantRuntimeProvider,
    CompositeAttachmentAdapter,
    SimpleImageAttachmentAdapter,
    SimpleTextAttachmentAdapter,
    Suggestions,
    ThreadMessageLike,
    WebSpeechDictationAdapter,
    useAui,
    useExternalStoreRuntime,
} from '@assistant-ui/react';
import {ReactNode, memo, useCallback, useEffect, useMemo, useState} from 'react';
import {useShallow} from 'zustand/shallow';

import {useChatStore} from '@/stores/useChatStore';
import type {PendingApprovalI} from '@/stores/useChatStore';
import {ApprovalResolutionContext} from '@/components/approvalResolutionContext';
import {useSSE} from '@/hooks/useSSE';
import {useAutomationChatVoiceSession} from '@/hooks/useAutomationChatVoiceSession';
import {checkVoiceSupport} from '@/lib/BrowserVoiceSession';
import {createWebhookVoiceAdapter} from '@/lib/ByteChefRealtimeVoiceAdapter';
import {extractStreamChunk} from '@/utils/stream-utils';
import {drainSseResponse} from '@/utils/sse-parser';
import {AutomationChatContext} from '@/hooks/useAutomationChatConfig';
import type {AutomationChatConfig} from '@/types';

// Mirrors `AskUserQuestionEventI` in client/src/shared/util/assistant-message-utils.ts. The widget
// cannot import from @/shared (it ships as an external npm package consumed by customer sites), so the
// shape is duplicated here; the formatter below matches `formatAskUserQuestionMessage` in that file
// byte-for-byte so questions render identically across the editor panel, AI Hub, and this widget.
interface AskUserQuestionOptionI {
    description: string;
    label: string;
}

interface AskUserQuestionI {
    header: string;
    multiSelect: boolean;
    options: AskUserQuestionOptionI[];
    question: string;
}

interface AskUserQuestionEventI {
    questions: AskUserQuestionI[];
    resumeUrl?: string;
}

// Mirrors `ApprovalRequestEventI` in client/src/shared/util/assistant-message-utils.ts (same
// cannot-import-from-@/shared constraint as above). Field-less approvals render as an inline
// Approve/Discard card (see ApprovalCard); approvals with form fields render as markdown with a link to
// the hosted approval form. Either way, resolution never happens through the chat input.
interface ApprovalRequestEventI {
    expiresAt?: string;
    formDescription?: string;
    formTitle?: string;
    formUrl?: string;
    inputs?: unknown[];
    resumeId: string;
}

function formatApprovalRequest(event: ApprovalRequestEventI): string {
    const title = event.formTitle || 'Approval requested';
    const description = event.formDescription ? `\n\n${event.formDescription}` : '';
    const expiry = event.expiresAt ? `\n\nExpires ${new Date(event.expiresAt).toLocaleString()}` : '';
    const link = event.formUrl ? `\n\n[Open the approval form](${event.formUrl})` : '';

    return `**${title}**${description}${expiry}${link}`;
}

function formatAskUserQuestion(event: AskUserQuestionEventI): string {
    if (!Array.isArray(event.questions) || event.questions.length === 0) {
        return '';
    }

    return event.questions
        .map((question) => {
            const options = Array.isArray(question.options) ? question.options : [];
            const optionLines = options
                .map((option, index) => `  ${index + 1}. **${option.label}** — ${option.description}`)
                .join('\n');

            const header = question.header ?? '';
            const text = question.question ?? '';
            const headerSegment = header ? `**${header}**: ` : '';

            return optionLines ? `${headerSegment}${text}\n${optionLines}` : `${headerSegment}${text}`;
        })
        .join('\n\n');
}

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => {
    return message;
};

const attachmentAdapter = new CompositeAttachmentAdapter([
    new SimpleImageAttachmentAdapter(),
    new SimpleTextAttachmentAdapter(),
]);

// Browser-native speech-to-text (Web Speech API). Replaces the former push-to-talk that POSTed audio to
// the webhook `${webhookUrl}/transcribe` endpoint. The composer's built-in Dictate/StopDictation buttons
// light up via `thread.capabilities.dictation` once this adapter is registered.
const dictationAdapter = new WebSpeechDictationAdapter();

type AutomationChatProviderProps = {
    children: ReactNode;
    config: AutomationChatConfig;
};

export const AutomationChatProvider = memo(function AutomationChatProvider({
    children,
    config,
}: AutomationChatProviderProps) {
    const {
        chatMode = false,
        description = 'How can I help you today?',
        suggestions,
        title = 'Hello there!',
        voiceMode = false,
        voiceWebhookUrl,
        webhookUrl,
    } = config;

    // checkVoiceSupport returns null when the browser meets every voice requirement (AudioContext,
    // AudioWorklet, getUserMedia, WebSocket, secure context). When it returns a reason string we disable
    // voice everywhere — the mic button hides, the voice context shape becomes undefined for the Thread.
    // Customer pages embedding the widget on an unsupported browser get a clean text-only experience
    // instead of a silent failure at first-click time.
    const voiceUnsupportedReason = useMemo(() => checkVoiceSupport(), []);
    const browserSupportsVoice = voiceUnsupportedReason === null;

    const voiceEnabled = Boolean(voiceWebhookUrl) && browserSupportsVoice;

    // When the workflow's trigger is `browser/v1/voiceSession`, the host app passes `config.voiceMode = true`.
    // In that case we install the RealtimeVoiceAdapter on the runtime (deriving the token endpoint from
    // webhookUrl) so that `VoiceModeLayout` can call `useVoiceControls` / `useVoiceState` inside the
    // AssistantRuntimeProvider tree.
    const realtimeVoiceAdapter = useMemo(
        () => (voiceMode ? createWebhookVoiceAdapter(webhookUrl) : undefined),
        [voiceMode, webhookUrl]
    );

    // contextValue is built after the voice hook below so it can include the live voice controls

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

    const voice = useAutomationChatVoiceSession({
        onEvent: (event) => {
            if (event.type === 'transcript_final' && typeof event.text === 'string' && event.text.length > 0) {
                setMessage({content: event.text, role: 'user'});
            } else if (event.type === 'assistant_text' && typeof event.text === 'string') {
                if (event.done) {
                    return;
                }

                appendToLastAssistantMessage(event.text);
            }
        },
        voiceWebhookUrl: voiceWebhookUrl ?? '',
    });

    // C3: cancel any in-flight SSE stream when voice goes active so the two transports cannot interleave
    // assistant text into the same message. The SSE side is single-tracked via streamRequest — clearing
    // it propagates through useSSE's effect cleanup.
    useEffect(() => {
        if (voice.status === 'active' || voice.status === 'connecting') {
            setStreamRequest(null);
            setIsRunning(false);
        }
    }, [voice.status]);

    const contextValue = useMemo(
        () => ({
            chatMode,
            description,
            suggestions,
            title,
            voice: voiceEnabled ? voice : undefined,
            voiceEnabled,
            voiceMode,
            webhookUrl,
        }),
        [chatMode, description, suggestions, title, voice, voiceEnabled, voiceMode, webhookUrl]
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
            // The hosted form page lives at /resume/{id}; the resolution endpoint at /job/resume/{id} —
            // the inverse of the server-side derivation in ApprovalRequestApprovalAction.
            const approvalResumeUrl = event.formUrl ? event.formUrl.replace('/resume/', '/job/resume/') : null;

            // Deliberately do NOT set the chat-input resume URL — typing never resolves an approval; only
            // the inline card's buttons or the linked hosted form do.
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
        [setMessage]
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

    // Continuation streaming for the inline approval card: resolve through the SSE-negotiated resume endpoint
    // and drain the resumed run's output into the conversation through the same event handlers as a normal turn.
    const resolveApproval = useCallback(
        async (pendingApproval: PendingApprovalI, payload: Record<string, unknown>) => {
            const response = await fetch(pendingApproval.resumeUrl, {
                body: JSON.stringify(payload),
                headers: {Accept: 'text/event-stream', 'Content-Type': 'application/json'},
                method: 'POST',
            });

            if (!response.ok) {
                throw new Error(`Resume request failed with status ${response.status}`);
            }

            const resolvedTitle = pendingApproval.formTitle || 'Approval requested';

            useChatStore.getState().setPendingApproval(null);
            setMessage({
                content: `**${resolvedTitle}** — ${payload.approved ? 'approved' : 'discarded'}.`,
                role: 'assistant',
            });

            const contentType = response.headers.get('content-type') ?? '';

            if (contentType.includes('text/event-stream')) {
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

                try {
                    const response = await fetch(currentResumeUrl, {
                        body: JSON.stringify({message: input}),
                        headers: {'Content-Type': 'application/json'},
                        method: 'POST',
                    });

                    if (!response.ok) {
                        throw new Error(`Resume request failed with status ${response.status}`);
                    }

                    const contentType = response.headers.get('content-type') ?? '';

                    if (contentType.includes('text/event-stream')) {
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
                    dictation: dictationAdapter,
                    ...(realtimeVoiceAdapter ? {voice: realtimeVoiceAdapter} : {}),
                },
                convertMessage,
                isRunning,
                messages,
                onNew,
            }),
            [isRunning, messages, onNew, realtimeVoiceAdapter]
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
