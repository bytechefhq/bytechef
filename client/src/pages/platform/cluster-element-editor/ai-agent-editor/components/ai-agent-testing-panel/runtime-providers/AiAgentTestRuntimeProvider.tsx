import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {SSERequestType, useSSE} from '@/shared/hooks/useSSE';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
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
import {ReactNode, useEffect, useState} from 'react';
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
    } = useAiAgentTestingChatStore(
        useShallow((state) => ({
            addToolExecution: state.addToolExecution,
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            conversationId: state.conversationId,
            messages: state.messages,
            setLastAssistantMessageContent: state.setLastAssistantMessageContent,
            setLastAssistantMessageError: state.setLastAssistantMessageError,
            setMessage: state.setMessage,
        }))
    );
    const {setJobKey} = useTestingModeStore();

    useAiAgentTestDataPills();

    const {connectionState, error: sseError} = useSSE(streamRequest, {
        eventHandlers: {
            error: (data) => {
                const errorMessage = typeof data === 'string' ? data : 'An unexpected error occurred';

                setLastAssistantMessageError(errorMessage);
                setIsRunning(false);
                setStreamRequest(null);
            },
            result: (data) => {
                if (typeof data === 'string' && data.trim().length > 0) {
                    setLastAssistantMessageContent(data);
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
    });

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

        setMessage({content: input, role: 'user'});
        setIsRunning(true);

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

    const runtime = useExternalStoreRuntime({
        adapters: {
            attachments: attachmentAdapter,
        },
        convertMessage,
        isRunning,
        messages,
        onNew,
    });

    return <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>;
}
