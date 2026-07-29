import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {ApprovalResolutionContext, ResumeError} from '@/shared/components/ai-chat/approvalResolutionContext';
import {SSERequestType, useSSE} from '@/shared/hooks/useSSE';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
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
import {ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useAiAgentTestingChatStore, useTestingModeStore} from '../../../stores';
import useAiAgentTestDataPills from '../hooks/useAiAgentTestDataPills';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => message;

const attachmentAdapter = new CompositeAttachmentAdapter([
    new SimpleImageAttachmentAdapter(),
    new SimpleTextAttachmentAdapter(),
]);

export default function AiAgentTestRuntimeProvider({children}: Readonly<{children: ReactNode}>) {
    const [isRunning, setIsRunning] = useState(false);
    const [streamRequest, setStreamRequest] = useState<SSERequestType>(null);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const {
        addToolExecution,
        appendToLastAssistantMessage,
        conversationId,
        messages,
        setLastAssistantMessageContent,
        setLastAssistantMessageError,
        setMessage,
        setResumeUrl,
        truncateMessagesFrom,
    } = useAiAgentTestingChatStore(
        useShallow((state) => ({
            addToolExecution: state.addToolExecution,
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            conversationId: state.conversationId,
            messages: state.messages,
            setLastAssistantMessageContent: state.setLastAssistantMessageContent,
            setLastAssistantMessageError: state.setLastAssistantMessageError,
            setMessage: state.setMessage,
            setResumeUrl: state.setResumeUrl,
            truncateMessagesFrom: state.truncateMessagesFrom,
        }))
    );
    const {setJobKey} = useTestingModeStore();

    useAiAgentTestDataPills();

    const pendingResumeRef = useRef<{reject: (error: Error) => void; resolve: () => void} | null>(null);

    const {connectionState, error: sseError} = useSSE(streamRequest, {
        eventHandlers: {
            approval_request: (data) => {
                if (typeof data !== 'object' || data === null || !('resumeId' in data)) {
                    console.error('Received malformed approval_request event:', data);

                    return;
                }

                const approvalEvent = data as ApprovalRequestEventI;

                // Render the interactive approval card (see ApprovalRequestMessage) so a gated tool suspending in a
                // test run is resolvable in-panel instead of hanging with no card. Typing never resolves it.
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
                setIsRunning(false);
                setStreamRequest(null);
            },
            error: (data) => {
                const errorMessage = typeof data === 'string' ? data : 'An unexpected error occurred';

                setLastAssistantMessageError(errorMessage);
                setIsRunning(false);
                setStreamRequest(null);
            },
            result: (data) => {
                if (typeof data === 'string' && data.trim().length > 0) {
                    setLastAssistantMessageContent(data);
                } else if (data !== null && typeof data === 'object') {
                    setLastAssistantMessageContent('```json\n' + JSON.stringify(data, null, 2) + '\n```');
                }

                setIsRunning(false);
                setStreamRequest(null);
            },
            start: (data) => {
                try {
                    const startData = typeof data === 'string' ? JSON.parse(data) : (data as {testId: string});

                    setJobKey(startData.testId);
                } catch (error) {
                    console.error('Failed to parse start event data:', error);

                    setLastAssistantMessageError('Failed to start the AI agent test. Please try again.');
                    setIsRunning(false);
                    setStreamRequest(null);
                }
            },
            stream: (data) => {
                const chunk = extractStreamChunk(data);

                if (chunk) {
                    appendToLastAssistantMessage(chunk);
                }
            },
            tool_execution: (data) => {
                if (typeof data !== 'object' || data === null || !('toolName' in data)) {
                    console.error('Received malformed tool_execution event:', data);

                    return;
                }

                const toolEvent = data as {
                    confidence: string;
                    inputs: Record<string, unknown>;
                    output: unknown;
                    reasoning: string;
                    toolName: string;
                };

                addToolExecution(toolEvent);
            },
        },
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

    const approvalResolution = useMemo(() => ({resolveApproval}), [resolveApproval]);

    useEffect(() => {
        if (connectionState === 'ERROR' && sseError) {
            setLastAssistantMessageError(`Connection to the AI agent test failed: ${sseError}`);
            setIsRunning(false);
            setStreamRequest(null);
        }
    }, [connectionState, sseError, setLastAssistantMessageError]);

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            throw new Error('Only text messages are supported');
        }

        const input = message.content[0].text;
        const currentResumeUrl = useAiAgentTestingChatStore.getState().resumeUrl;

        setMessage({content: input, role: 'user'});
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

                setLastAssistantMessageError('Failed to submit your answer. Please try again.');
            } finally {
                setIsRunning(false);
            }

            return;
        }

        try {
            setMessage({content: [], role: 'assistant'} as ThreadMessageLike);

            const request: SSERequestType = {
                init: {
                    body: JSON.stringify({
                        attachments: message.attachments ?? [],
                        conversationId,
                        environmentId: currentEnvironmentId,
                        message: input,
                        workflowId: workflow.id,
                        workflowNodeName: rootClusterElementNodeData?.workflowNodeName,
                    }),
                    headers: {'Content-Type': 'application/json'},
                    method: 'POST',
                },
                url: '/api/platform/internal/ai-agent-tests',
            };

            setStreamRequest(request);
        } catch (error) {
            console.error('Failed to build AI agent test request:', error);

            setLastAssistantMessageError('Failed to send your message. Please try again.');
            setIsRunning(false);
        }
    };

    const onEdit = async (message: AppendMessage) => {
        const parentIndex = message.parentId != null ? Number.parseInt(message.parentId, 10) : -1;
        const editedIndex = Number.isNaN(parentIndex) ? messages.length : parentIndex + 1;

        truncateMessagesFrom(editedIndex);

        await onNew(message);
    };

    const runtime = useExternalStoreRuntime({
        adapters: {
            attachments: attachmentAdapter,
        },
        convertMessage,
        isRunning,
        messages,
        onEdit,
        onNew,
    });

    return (
        <ApprovalResolutionContext.Provider value={approvalResolution}>
            <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>
        </ApprovalResolutionContext.Provider>
    );
}
