import {ApprovalResolutionContext, ResumeError} from '@/shared/components/ai-chat/approvalResolutionContext';
import {useSSE} from '@/shared/hooks/useSSE';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {
    ApprovalRequestEventI,
    AskUserQuestionEventI,
    appendToLastAssistantMessage,
    formatAskUserQuestionMessage,
    setLastAssistantMessageContent,
} from '@/shared/util/assistant-message-utils';
import {generateRandomId} from '@/shared/util/random-utils';
import {extractStreamChunk} from '@/shared/util/stream-utils';
import {getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
import {
    AppendMessage,
    AssistantRuntimeProvider,
    CompositeAttachmentAdapter,
    SimpleImageAttachmentAdapter,
    SimpleTextAttachmentAdapter,
    type SuggestionConfig,
    Suggestions,
    ThreadMessageLike,
    useAui,
    useExternalStoreRuntime,
} from '@assistant-ui/react';
import {ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';

/**
 * Every agent's generated draft workflow names its chat trigger `chat_1` — unlike a hand-authored workflow
 * (the workflow editor's test chat targets `trigger_1`). The test-run payload keys its input under this
 * constant so the request lands on the right trigger node.
 */
export const AGENT_TEST_CHAT_TRIGGER_NODE_NAME = 'chat_1';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => message;

const attachmentAdapter = new CompositeAttachmentAdapter([
    new SimpleImageAttachmentAdapter(),
    new SimpleTextAttachmentAdapter(),
]);

export interface BuildAgentTestChatStreamRequestPropsI {
    attachments?: AppendMessage['attachments'];
    conversationId: string | undefined;
    environmentId: number;
    message: string;
    workflowId: string;
}

/**
 * Builds the SSE POST request for a single draft-agent test-chat turn. Kept as a standalone, side-effect-free
 * function (no React) so its URL/body shape can be asserted directly in tests without mounting the runtime
 * provider or assistant-ui.
 */
export function buildAgentTestChatStreamRequest({
    attachments,
    conversationId,
    environmentId,
    message,
    workflowId,
}: BuildAgentTestChatStreamRequestPropsI) {
    return getTestWorkflowStreamPostRequest({
        environmentId,
        id: workflowId,
        testWorkflowRequest: {
            inputs: {
                [AGENT_TEST_CHAT_TRIGGER_NODE_NAME]: {
                    attachments,
                    conversationId,
                    message,
                },
            },
        },
    });
}

export interface UseAgentTestChatRuntimeResultI {
    conversationId: string;
    isRunning: boolean;
    messages: ThreadMessageLike[];
    onNew: (message: AppendMessage) => Promise<void>;
    resolveApproval: (resumeId: string, payload: Record<string, unknown>) => Promise<void>;
    runtime: ReturnType<typeof useExternalStoreRuntime>;
}

/**
 * Owns the message state and SSE plumbing for the agent detail page's draft test-chat panel, entirely in
 * local component state — deliberately NOT the workflow editor's {@code useWorkflowTestChatStore}, which is
 * a page-scoped zustand singleton whose {@code messages}/{@code conversationId} persist across route
 * changes. Reusing it here would leak an agent's test conversation into the workflow editor's own test-chat
 * panel (and vice versa) the next time either mounts. Local state keeps every panel instance isolated by
 * construction, at the cost of not surviving a page refresh — an acceptable trade-off for a lightweight
 * draft-preview surface.
 */
export function useAgentTestChatRuntime(workflowId: string): UseAgentTestChatRuntimeResultI {
    const [messages, setMessages] = useState<ThreadMessageLike[]>([]);
    const [isRunning, setIsRunning] = useState(false);
    const [resumeUrl, setResumeUrl] = useState<string | null>(null);
    const [streamRequest, setStreamRequest] = useState<{url: string; init?: RequestInit} | null>(null);

    // Generated once for the lifetime of this hook instance and reused for every turn so the server can
    // thread multi-turn memory across sends. AgentDetail keys its content on `agent.id`, so navigating to a
    // different agent remounts this hook and mints a fresh conversation.
    const [conversationId] = useState(() => generateRandomId());

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const setMessage = useCallback((message: ThreadMessageLike) => {
        setMessages((previousMessages) => [...previousMessages, message]);
    }, []);

    const pendingResumeRef = useRef<{reject: (error: Error) => void; resolve: () => void} | null>(null);

    const eventHandlers = useMemo(
        () => ({
            approval_request: (data: unknown) => {
                if (typeof data !== 'object' || data === null || !('resumeId' in data)) {
                    console.error('Received malformed approval_request event:', data);

                    return;
                }

                const approvalEvent = data as ApprovalRequestEventI;

                // Render an interactive approval card that resolves through the job-resume endpoint.
                // Deliberately do NOT set a resume URL for the chat input — typing never resolves an
                // approval; only the card (or the hosted form) does.
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
            ask_user_question: (data: unknown) => {
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

                setMessages((previousMessages) =>
                    setLastAssistantMessageContent(previousMessages, formatAskUserQuestionMessage(questionEvent))
                );
                setResumeUrl(questionEvent.resumeUrl ?? null);
                setIsRunning(false);
                setStreamRequest(null);
            },
            error: () => {
                setIsRunning(false);
                setStreamRequest(null);
            },
            result: (data: unknown) => {
                try {
                    const resultData = typeof data === 'string' ? JSON.parse(data) : data;
                    const message = (resultData as {job?: {outputs?: {message?: string}}})?.job?.outputs?.message ?? '';

                    // Do not overwrite streamed content with empty final text.
                    if (message && message.trim().length > 0) {
                        setMessages((previousMessages) => setLastAssistantMessageContent(previousMessages, message));
                    }
                } catch (error) {
                    console.error('Failed to parse agent test chat result:', error);
                } finally {
                    setIsRunning(false);
                    setStreamRequest(null);
                }
            },
            stream: (data: unknown) => {
                const chunk = extractStreamChunk(data);

                if (chunk) {
                    setMessages((previousMessages) => appendToLastAssistantMessage(previousMessages, chunk));
                }
            },
        }),
        [setMessage]
    );

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

    useEffect(() => {
        if (connectionState === 'CLOSED' || connectionState === 'ERROR') {
            setIsRunning(false);
        }
    }, [connectionState]);

    // Reset isRunning on unmount so a panel that unmounts mid-turn (e.g. navigating away) never leaves a
    // stale "running" flag behind for the next mount to inherit.
    useEffect(() => {
        return () => setIsRunning(false);
    }, []);

    const onNew = useCallback(
        async (message: AppendMessage) => {
            if (message.content[0]?.type !== 'text') {
                throw new Error('Only text messages are supported');
            }

            const input = message.content[0].text;

            setMessage({attachments: [...(message.attachments ?? [])], content: input, role: 'user'});
            setIsRunning(true);

            if (resumeUrl) {
                try {
                    setResumeUrl(null);

                    const response = await fetch(resumeUrl, {
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

            try {
                // Prepare an empty assistant message so streaming appears immediately.
                setMessage({content: '', role: 'assistant'});

                const request = buildAgentTestChatStreamRequest({
                    attachments: message.attachments,
                    conversationId,
                    environmentId: currentEnvironmentId,
                    message: input,
                    workflowId,
                });

                setStreamRequest(request);
            } catch (error) {
                console.error('Failed to build agent test chat stream request:', error);

                setIsRunning(false);
            }
        },
        [conversationId, currentEnvironmentId, resumeUrl, setMessage, workflowId]
    );

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
        [setMessage]
    );

    const runtime = useExternalStoreRuntime(
        useMemo(
            () => ({
                adapters: {attachments: attachmentAdapter},
                convertMessage,
                isRunning,
                messages,
                onNew,
            }),
            [isRunning, messages, onNew]
        )
    );

    return {conversationId, isRunning, messages, onNew, resolveApproval, runtime};
}

export function AgentTestChatRuntimeProvider({
    children,
    suggestions,
    workflowId,
}: Readonly<{
    children: ReactNode;
    suggestions?: SuggestionConfig[];
    workflowId: string;
}>) {
    const {resolveApproval, runtime} = useAgentTestChatRuntime(workflowId);

    const approvalResolution = useMemo(() => ({resolveApproval}), [resolveApproval]);

    // Suggestions reach <Thread> through the aui store rather than a prop, which is why they are wired here
    // and not on the panel — the same seam CopilotRuntimeProvider uses.
    const aui = useAui({suggestions: Suggestions(suggestions ?? [])});

    return (
        <ApprovalResolutionContext.Provider value={approvalResolution}>
            <AssistantRuntimeProvider aui={aui} runtime={runtime}>
                {children}
            </AssistantRuntimeProvider>
        </ApprovalResolutionContext.Provider>
    );
}
