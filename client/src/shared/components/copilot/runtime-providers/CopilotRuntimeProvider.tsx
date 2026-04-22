import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {GetWorkspaceFilesQuery, WorkspaceFileSource} from '@/shared/middleware/graphql';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {getCookie} from '@/shared/util/cookie-utils';
import {getRandomId} from '@/shared/util/random-utils';
import {AgentSubscriber, HttpAgent} from '@ag-ui/client';
import {AppendMessage, AssistantRuntimeProvider, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {QueryClient, useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useParams} from 'react-router-dom';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => {
    return message;
};

interface CreateWorkspaceFileToolResultI {
    downloadUrl: string;
    id: number | string;
    name: string;
    sizeBytes: number | string;
}

interface BuildCopilotSubscriberDepsI {
    appendToLastAssistantMessage: (text: string) => void;
    queryClient: QueryClient;
}

const parseToolContent = (content: string): (Partial<CreateWorkspaceFileToolResultI> & {error?: string}) | null => {
    try {
        return JSON.parse(content);
    } catch {
        return null;
    }
};

export const buildCopilotSubscriber = ({
    appendToLastAssistantMessage,
    queryClient,
}: BuildCopilotSubscriberDepsI): AgentSubscriber => {
    const toolCallNamesById = new Map<string, string>();
    const toolCallArgsById = new Map<string, Record<string, unknown>>();

    return {
        onTextMessageContentEvent: ({event, textMessageBuffer}) => {
            appendToLastAssistantMessage(textMessageBuffer + event.delta);
        },
        onTextMessageEndEvent: ({textMessageBuffer}) => {
            appendToLastAssistantMessage(textMessageBuffer);
        },
        onToolCallEndEvent: ({event, toolCallArgs}) => {
            toolCallArgsById.set(event.toolCallId, toolCallArgs ?? {});
        },
        onToolCallResultEvent: ({event}) => {
            const toolCallName = toolCallNamesById.get(event.toolCallId);

            if (toolCallName !== 'createWorkspaceFile') {
                return;
            }

            const parsed = parseToolContent(event.content);

            if (!parsed || parsed.error || parsed.id == null || !parsed.name) {
                return;
            }

            const parsedResult = parsed as CreateWorkspaceFileToolResultI;
            const args = toolCallArgsById.get(event.toolCallId) ?? {};
            const mimeType = typeof args.mimeType === 'string' ? args.mimeType : 'application/octet-stream';
            const prompt = typeof args.description === 'string' ? args.description : null;

            const newFile: GetWorkspaceFilesQuery['workspaceFiles'][number] = {
                __typename: 'WorkspaceFile',
                createdBy: null,
                createdDate: Date.now(),
                description: prompt,
                downloadUrl: parsedResult.downloadUrl,
                generatedByAgentSource: null,
                generatedFromPrompt: prompt,
                id: String(parsedResult.id),
                lastModifiedBy: null,
                lastModifiedDate: Date.now(),
                mimeType,
                name: parsedResult.name,
                sizeBytes: parsedResult.sizeBytes,
                source: WorkspaceFileSource.AiGenerated,
                tags: [],
            };

            queryClient.setQueriesData<GetWorkspaceFilesQuery | undefined>(
                {queryKey: ['GetWorkspaceFiles']},
                (existing) => {
                    if (!existing) {
                        return existing;
                    }

                    return {
                        ...existing,
                        workspaceFiles: [newFile, ...existing.workspaceFiles],
                    };
                }
            );

            void queryClient.invalidateQueries({queryKey: ['GetWorkspaceFiles']});

            toast.success(`Created "${parsedResult.name}"`);

            toolCallNamesById.delete(event.toolCallId);
            toolCallArgsById.delete(event.toolCallId);
        },
        onToolCallStartEvent: ({event}) => {
            toolCallNamesById.set(event.toolCallId, event.toolCallName);
        },
    };
};

export function CopilotRuntimeProvider({
    children,
}: Readonly<{
    children: ReactNode;
}>) {
    const [isRunning, setIsRunning] = useState(false);

    const {addMessage, appendToLastAssistantMessage, context, conversationId, messages} = useCopilotStore(
        useShallow((state) => ({
            addMessage: state.addMessage,
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            context: state.context,
            conversationId: state.conversationId,
            messages: state.messages,
        }))
    );
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const currentComponent = useWorkflowNodeDetailsPanelStore((state) => state.currentComponent);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {projectId, projectWorkflowId} = useParams();

    const sourceKey = context?.source ?? Source.WORKFLOW_EDITOR;

    const agent = new HttpAgent({
        agentId: Source[sourceKey],
        headers: {
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
        threadId: conversationId!,
        url: `/api/platform/internal/ai/chat/${Source[sourceKey].toLowerCase()}`,
    });

    const queryClient = useQueryClient();

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            throw new Error('Only text messages are supported');
        }

        const input = message.content[0].text;

        addMessage({content: input, role: 'user'});
        setIsRunning(true);

        agent.addMessage({
            content: input,
            id: getRandomId(),
            role: 'user',
        });

        const {workflowExecutionError, ...contextWithoutError} = (context ?? {}) as typeof context & {
            workflowExecutionError?: {
                errorMessage?: string;
                stackTrace?: string[];
                title?: string;
                workflowId?: string;
            };
        };

        const stateToSend = {
            ...contextWithoutError,
            currentSelectedNode: currentComponent?.name,
            workflowId: workflow.id,
            workspaceId: currentWorkspaceId,
            ...(workflow.id === workflowExecutionError?.workflowId
                ? {workflowExecutionError: workflowExecutionError}
                : {}),
        };

        agent.setState(stateToSend);

        // Prepare an empty assistant message to stream into
        addMessage({content: '', role: 'assistant'});

        const subscriber = buildCopilotSubscriber({appendToLastAssistantMessage, queryClient});

        await agent.runAgent(
            {
                runId: getRandomId(),
            },
            subscriber
        );

        setIsRunning(false);

        // if (workflowUpdated) {
        queryClient.invalidateQueries({
            queryKey: ProjectWorkflowKeys.projectWorkflow(+projectId!, +projectWorkflowId!),
        });
        // }
    };

    const runtime = useExternalStoreRuntime({
        convertMessage,
        isRunning,
        messages,
        onNew,
    });

    return <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>;
}
