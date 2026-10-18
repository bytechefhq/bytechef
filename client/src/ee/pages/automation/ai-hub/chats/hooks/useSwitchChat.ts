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

const TOOL_APPROVAL_CONTINUATION_PATTERN = /^\[tool-approval #(\d+)[\s\]]/;

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

/**
 * Rebuilds the message parts for the tool activity persisted alongside a transcript row: one `tool-call` part per call
 * plus, when the call has a persisted result that maps to an interactive or informational card, the same data part
 * the live stream would have appended. `allowInteractive` is true only for rows after the last user message, so a
 * pending question comes back answerable while questions the user already moved past stay as transcript text.
 */
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

/**
 * Maps the server transcript rows to thread messages, reattaching the persisted tool activity. Assistant rows get
 * their tool parts appended after the text; a user row's tool activity hoists into a synthetic assistant message right
 * after it, since tool-call parts only render on assistant messages.
 */
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

function isToolApprovalRequestPart(part: unknown): part is {data: Record<string, unknown>; type: string} {
    return (
        typeof part === 'object' && part !== null && (part as {type?: unknown}).type === 'data-tool-approval-request'
    );
}

function readContinuationApprovalId(message: ThreadMessageLike): number | undefined {
    if (message.role !== 'user') {
        return undefined;
    }

    const text =
        typeof message.content === 'string'
            ? message.content
            : message.content.map((part) => (part.type === 'text' && 'text' in part ? part.text : '')).join('');

    const match = TOOL_APPROVAL_CONTINUATION_PATTERN.exec(text);

    return match ? Number(match[1]) : undefined;
}

function collectToolApprovalCardIds(messages: ThreadMessageLike[]): Set<number> {
    const approvalIds = new Set<number>();

    for (const message of messages) {
        if (message.role !== 'assistant' || !Array.isArray(message.content)) {
            continue;
        }

        for (const part of message.content) {
            if (isToolApprovalRequestPart(part) && typeof part.data.approvalId === 'number') {
                approvalIds.add(part.data.approvalId);
            }
        }
    }

    return approvalIds;
}

function parseApprovalArguments(argumentsJson: string): Record<string, unknown> {
    try {
        const parsedArguments = JSON.parse(argumentsJson);

        return parsedArguments != null && typeof parsedArguments === 'object'
            ? (parsedArguments as Record<string, unknown>)
            : {};
    } catch {
        return {};
    }
}

function toToolApprovalCardMessage(approval: AiHubToolApprovalI): ThreadMessageLike {
    return {
        content: [
            {
                data: {
                    approvalId: approval.id,
                    arguments: parseApprovalArguments(approval.arguments),
                    awaitingApproval: true,
                    componentName: approval.componentName,
                    kind: 'tool-approval-request',
                    toolName: approval.toolName,
                },
                type: 'data-tool-approval-request',
            },
        ],
        role: 'assistant',
    } as ThreadMessageLike;
}

/**
 * Puts back the approval cards the transcript itself cannot restore. A gated call pauses the run before its result is
 * written to session memory, so the history usually holds only the gated call and, once decided, the
 * {@code [tool-approval #N ...]} continuation prompt the server posted. That prompt is replaced by the approval's
 * card, matching what the live stream showed; when the card did come back from a persisted result, the prompt is
 * dropped instead. A still-pending approval has no continuation yet, so its card goes at the end. Without the
 * approvals list (lookup failed) the transcript is left as is.
 */
function restoreToolApprovalCards(
    messages: ThreadMessageLike[],
    approvalsById: Map<number, AiHubToolApprovalI>
): ThreadMessageLike[] {
    if (approvalsById.size === 0) {
        return messages;
    }

    const cardApprovalIds = collectToolApprovalCardIds(messages);
    const restored: ThreadMessageLike[] = [];

    for (const message of messages) {
        const continuationApprovalId = readContinuationApprovalId(message);
        const approval = continuationApprovalId != null ? approvalsById.get(continuationApprovalId) : undefined;

        if (!approval) {
            restored.push(message);

            continue;
        }

        if (!cardApprovalIds.has(approval.id)) {
            restored.push(toToolApprovalCardMessage(approval));

            cardApprovalIds.add(approval.id);
        }
    }

    for (const approval of approvalsById.values()) {
        if (approval.status === 'PENDING' && !cardApprovalIds.has(approval.id)) {
            restored.push(toToolApprovalCardMessage(approval));
        }
    }

    return restored;
}

/**
 * Stitches settled tool-approval outcomes onto the restored `data-tool-approval-request` parts. The restored part only
 * carries the original gated request, so without this a decided approval would render its Approve / Reject buttons
 * forever. A PENDING approval is left untouched so its card stays interactive.
 */
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
            if (!isToolApprovalRequestPart(part)) {
                return part;
            }

            const data = part.data;
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

/**
 * Maps a durable workflow artifact back to the `openWorkflowTab` tool call that rendered its link card in the
 * transcript. The created / updated rows carry the same project metadata as a referenced one, and the agent almost
 * always opened the workflow it built, so all three get a card. Other kinds return null.
 */
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

/**
 * Rebuilds the artifact link cards that streamed live but were lost on reload: the cards are tool-call UI and never
 * persisted in chat memory, but the underlying open is recorded as an `ai_hub_chat_artifact` row. One synthetic
 * assistant message carries a `tool-call` part per openable artifact, appended after the text transcript.
 */
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

/**
 * Fetches a chat's history, artifacts and tool approvals and maps them to the thread messages the hub store holds.
 * Artifacts and tool approvals degrade to empty on their own failure; only the message fetch can reject.
 */
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
        ...applyToolApprovalStatuses(
            restoreToolApprovalCards(mapServerMessages(messages), toolApprovalsById),
            toolApprovalsById
        ),
        ...buildArtifactLinkMessages(artifacts),
    ];
}

/**
 * Switches the hub to a chat. The UI switches immediately (thread id, selected chat, route) and the history loads
 * after, so a click never blocks on the fetch. Returns whether the history loaded; a failure is reported as a toast
 * and leaves the user on the chat with an empty thread. Memoised on the workspace so callers can put it in effect
 * dependencies.
 */
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
