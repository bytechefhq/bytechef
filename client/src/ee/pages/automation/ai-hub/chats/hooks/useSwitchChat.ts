import {isWorkflowArtifactKind, parseArtifactMetadata} from '@/ee/pages/automation/ai-hub/artifacts/artifactOpen';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {ARTIFACT_OPEN_TOOL_NAMES} from '@/ee/pages/automation/ai-hub/messages/AiHubToolCallRenderer';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {toToolResultDataPart} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import {reportMutationError} from '@/shared/error/useReportQueryError';
import {ThreadMessageLike} from '@assistant-ui/react';
import {useCallback} from 'react';
import {useNavigate} from 'react-router-dom';

import {
    AiHubChatArtifactI,
    AiHubChatI,
    AiHubChatMessageI,
    AiHubToolApprovalI,
    getChatArtifacts,
    getChatMessages,
    getToolApprovals,
} from '../api/chats.api';

interface RestoredToolEventI {
    arguments?: string;
    id?: string;
    kind: string;
    name?: string;
    response?: string;
}

interface OpenToolCallI {
    args: Record<string, unknown>;
    toolName: string;
}

const INTERACTIVE_DATA_PART_TYPES = new Set([
    'data-ask-user-question',
    'data-create-connection',
    'data-select-connection',
    'data-select-property-option',
]);

function parseRestoredToolEvents(toolEventsJson: string | null): RestoredToolEventI[] {
    if (!toolEventsJson) {
        return [];
    }

    try {
        const parsed = JSON.parse(toolEventsJson);

        return Array.isArray(parsed)
            ? parsed.filter((event): event is RestoredToolEventI => event != null && typeof event === 'object')
            : [];
    } catch {
        console.warn('[AiHubChats] Unparseable toolEventsJson, skipping tool-card restore for this row');

        return [];
    }
}

type RestoredPartType = Exclude<ThreadMessageLike['content'], string>[number];

function buildRestoredToolParts(
    events: RestoredToolEventI[],
    {allowInteractive, rowIndex}: {allowInteractive: boolean; rowIndex: number}
): RestoredPartType[] {
    const resultsById = new Map<string, RestoredToolEventI>();

    for (const event of events) {
        if (event.kind === 'result' && event.id) {
            resultsById.set(event.id, event);
        }
    }

    const parts: RestoredPartType[] = [];

    events.forEach((event, eventIndex) => {
        if (event.kind !== 'call' || !event.name || ARTIFACT_OPEN_TOOL_NAMES.has(event.name)) {
            return;
        }

        const resultEvent = event.id ? resultsById.get(event.id) : undefined;

        let args: Record<string, unknown> | undefined;

        if (event.arguments) {
            try {
                const parsedArguments = JSON.parse(event.arguments);

                args =
                    parsedArguments != null && typeof parsedArguments === 'object'
                        ? (parsedArguments as Record<string, unknown>)
                        : undefined;
            } catch {
                args = undefined;
            }
        }

        let result: unknown;

        if (resultEvent?.response) {
            try {
                result = JSON.parse(resultEvent.response);
            } catch {
                result = resultEvent.response;
            }
        }

        parts.push({
            args: args ?? {},
            argsText: event.arguments ?? '',
            result,
            toolCallId: event.id || `restored-tool-${rowIndex}-${eventIndex}`,
            toolName: event.name,
            type: 'tool-call' as const,
        } as RestoredPartType);

        if (resultEvent?.response) {
            const dataPart = toToolResultDataPart(event.name, resultEvent.response);

            if (dataPart?.ok && (allowInteractive || !INTERACTIVE_DATA_PART_TYPES.has(dataPart.type))) {
                parts.push({data: dataPart.data, type: dataPart.type as `data-${string}`} as RestoredPartType);
            }
        }
    });

    return parts;
}

function mapServerRoleToClient(role: string): 'user' | 'assistant' | 'system' | null {
    const normalized = role.toLowerCase();

    if (normalized === 'tool') {
        return null;
    }

    if (normalized === 'user' || normalized === 'assistant' || normalized === 'system') {
        return normalized;
    }

    console.warn(`[AiHubChats] Unknown message role from server: "${role}"`);

    return null;
}

function mapServerMessages(messages: AiHubChatMessageI[]): ThreadMessageLike[] {
    const lastUserRowIndex = messages.reduce(
        (lastIndex, message, index) => (mapServerRoleToClient(message.role) === 'user' ? index : lastIndex),
        -1
    );

    const mapped: ThreadMessageLike[] = [];

    messages.forEach((serverMessage, rowIndex) => {
        const clientRole = mapServerRoleToClient(serverMessage.role);

        if (clientRole === null) {
            return;
        }

        const toolParts = buildRestoredToolParts(parseRestoredToolEvents(serverMessage.toolEventsJson), {
            allowInteractive: rowIndex >= lastUserRowIndex,
            rowIndex,
        });

        if (clientRole !== 'assistant') {
            mapped.push({content: serverMessage.content, role: clientRole});

            if (toolParts.length > 0) {
                mapped.push({content: toolParts, role: 'assistant'});
            }

            return;
        }

        if (toolParts.length === 0) {
            mapped.push({content: serverMessage.content, role: clientRole});

            return;
        }

        const parts: RestoredPartType[] = [];

        if (serverMessage.content && serverMessage.content.trim()) {
            parts.push({text: serverMessage.content, type: 'text' as const});
        }

        parts.push(...toolParts);

        mapped.push({content: parts, role: 'assistant'});
    });

    return mapped;
}

function applyToolApprovalStatuses(
    messages: ThreadMessageLike[],
    approvalsById: Map<number, AiHubToolApprovalI>
): ThreadMessageLike[] {
    if (approvalsById.size === 0) {
        return messages;
    }

    return messages.map((message) => {
        if (message.role !== 'assistant' || !Array.isArray(message.content)) {
            return message;
        }

        let changed = false;

        const content = message.content.map((part) => {
            if (
                typeof part !== 'object' ||
                part === null ||
                (part as {type?: unknown}).type !== 'data-tool-approval-request'
            ) {
                return part;
            }

            const data = (part as {data: Record<string, unknown>}).data;
            const approvalId = typeof data.approvalId === 'number' ? data.approvalId : null;
            const approval = approvalId != null ? approvalsById.get(approvalId) : undefined;

            if (!approval || approval.status === 'PENDING') {
                return part;
            }

            changed = true;

            return {
                ...part,
                data: {
                    ...data,
                    executionError: approval.executionError ?? undefined,
                    resolvedBy: approval.decidedByUserId != null ? String(approval.decidedByUserId) : undefined,
                    resolvedStatus: approval.status,
                },
            };
        });

        return changed ? ({...message, content} as ThreadMessageLike) : message;
    });
}

function artifactToOpenToolCall(artifact: AiHubChatArtifactI): OpenToolCallI | null {
    if (!isWorkflowArtifactKind(artifact.kind)) {
        return null;
    }

    const metadata = parseArtifactMetadata(artifact.metadataJson);

    return {
        args: {
            name: artifact.artifactName,
            projectId: metadata['projectId'],
            projectWorkflowId: Number(metadata['projectWorkflowId'] ?? 0),
            workflowId: artifact.artifactId,
        },
        toolName: 'openWorkflowTab',
    };
}

function buildArtifactLinkMessages(artifacts: AiHubChatArtifactI[]): ThreadMessageLike[] {
    const seen = new Set<string>();

    const parts = artifacts
        .map((artifact) => ({artifact, openCall: artifactToOpenToolCall(artifact)}))
        .filter((entry): entry is {artifact: AiHubChatArtifactI; openCall: OpenToolCallI} => entry.openCall !== null)
        .filter(({openCall}) => {
            const key = `${openCall.toolName}|${JSON.stringify(openCall.args)}`;

            if (seen.has(key)) {
                return false;
            }

            seen.add(key);

            return true;
        })
        .map(({artifact, openCall}) => ({
            args: openCall.args,
            argsText: JSON.stringify(openCall.args),
            result: {opened: true},
            toolCallId: `rehydrated-artifact-${artifact.id}`,
            toolName: openCall.toolName,
            type: 'tool-call' as const,
        }));

    if (parts.length === 0) {
        return [];
    }

    return [{content: parts, role: 'assistant'} as ThreadMessageLike];
}

async function loadChatTranscript({
    chatId,
    workspaceId,
}: {
    chatId: number;
    workspaceId: number;
}): Promise<ThreadMessageLike[]> {
    const [messages, artifacts, toolApprovals] = await Promise.all([
        getChatMessages({chatId, workspaceId}),
        getChatArtifacts({chatId, workspaceId}).catch((): AiHubChatArtifactI[] => []),
        getToolApprovals({chatId, workspaceId}).catch((): AiHubToolApprovalI[] => []),
    ]);

    const toolApprovalsById = new Map(toolApprovals.map((approval) => [approval.id, approval]));

    return [
        ...applyToolApprovalStatuses(mapServerMessages(messages), toolApprovalsById),
        ...buildArtifactLinkMessages(artifacts),
    ];
}

export function useSwitchChat() {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const navigate = useNavigate();

    return useCallback(
        async (chat: AiHubChatI): Promise<boolean> => {
            aiHubStore.setState({
                chatId: chat.threadId,
                messages: [],
                messagesLoading: true,
            });

            aiHubChatsStore.getState().setCurrentChatId(chat.id);

            navigate(`/automation/ai-hub/chats/${chat.id}`);

            try {
                const loadedMessages = await loadChatTranscript({chatId: chat.id, workspaceId: currentWorkspaceId});

                if (aiHubStore.getState().chatId === chat.threadId) {
                    aiHubStore.setState({
                        messages: loadedMessages,
                        messagesLoading: false,
                    });
                }

                return true;
            } catch (error) {
                if (aiHubStore.getState().chatId === chat.threadId) {
                    aiHubStore.setState({messagesLoading: false});
                }

                reportMutationError('Switch chat', error instanceof Error ? error : new Error(String(error)));

                return false;
            }
        },
        [currentWorkspaceId, navigate]
    );
}
