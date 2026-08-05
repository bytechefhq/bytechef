import {ARTIFACT_OPEN_TOOL_NAMES} from '@/ee/pages/automation/ai-hub/messages/AiHubToolCallRenderer';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {aiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {toToolResultDataPart} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import {reportMutationError} from '@/shared/error/useReportQueryError';
import {ThreadMessageLike} from '@assistant-ui/react';
import {useCallback} from 'react';
import {useNavigate} from 'react-router-dom';

import {AiHubTaskArtifactI, AiHubTaskI, AiHubTaskMessageI, getTaskArtifacts, getTaskMessages} from '../api/tasks.api';

interface RestoredToolEventI {
    arguments?: string;
    id?: string;
    kind: string;
    name?: string;
    response?: string;
}

// Interactive prompt cards must not resurrect once the conversation moved past them — re-showing an
// already-answered askUserQuestion wizard (the answered-state store is ephemeral) or a connection picker
// would invite duplicate answers. Informational cards (citations) restore everywhere.
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
        console.warn('[AiHubTasks] Unparseable toolEventsJson, skipping tool-card restore for this row');

        return [];
    }
}

type RestoredPartType = Exclude<ThreadMessageLike['content'], string>[number];

/**
 * Rebuild the message parts for the tool activity persisted alongside a transcript row: one `tool-call`
 * part per call (renders the collapsible tool card via AiHubToolCallFallback, subject to the show-tool-calls
 * toggle) plus, when the call has a persisted result that maps to an interactive/informational card
 * (askUserQuestion and friends), the same data part the live stream would have appended. `allowInteractive`
 * is true only for rows after the last user message — a pending question card comes back answerable, while
 * questions the user already moved past stay as transcript text.
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
        if (event.kind !== 'call' || !event.name) {
            return;
        }

        // Artifact link cards are rebuilt from the durable artifact rows (buildArtifactLinkMessages) —
        // restoring them here too would render each link twice.
        if (ARTIFACT_OPEN_TOOL_NAMES.has(event.name)) {
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

/**
 * Map the server transcript rows to thread messages, reattaching the persisted tool activity. Assistant rows
 * get their tool parts appended after the text; a USER row's tool activity (tool turns that ran before the
 * next assistant text landed) hoists into a synthetic assistant message right after it, since tool-call parts
 * only render on assistant messages.
 */
function mapServerMessages(messages: AiHubTaskMessageI[]): ThreadMessageLike[] {
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
            mapped.push({content: serverMessage.content, role: clientRole} as ThreadMessageLike);

            if (toolParts.length > 0) {
                mapped.push({content: toolParts, role: 'assistant'} as ThreadMessageLike);
            }

            return;
        }

        if (toolParts.length === 0) {
            mapped.push({content: serverMessage.content, role: clientRole} as ThreadMessageLike);

            return;
        }

        const parts: RestoredPartType[] = [];

        if (serverMessage.content && serverMessage.content.trim()) {
            parts.push({text: serverMessage.content, type: 'text' as const});
        }

        parts.push(...toolParts);

        mapped.push({content: parts, role: 'assistant'} as ThreadMessageLike);
    });

    return mapped;
}

function mapServerRoleToClient(role: string): 'user' | 'assistant' | 'system' | null {
    const normalized = role.toLowerCase();

    if (normalized === 'tool') {
        return null;
    }

    if (normalized === 'user' || normalized === 'assistant' || normalized === 'system') {
        return normalized;
    }

    // Server schema drift (a new role shipped without a client update) would otherwise drop the message
    // silently. Warn so it surfaces in the console instead of vanishing into the filter() call.
    console.warn(`[AiHubTasks] Unknown message role from server: "${role}"`);

    return null;
}

function parseArtifactMetadata(metadataJson: string | null): Record<string, string> {
    if (!metadataJson) {
        return {};
    }

    try {
        const parsed = JSON.parse(metadataJson);

        return parsed != null && typeof parsed === 'object' ? (parsed as Record<string, string>) : {};
    } catch {
        return {};
    }
}

/**
 * Map a durable {@link AiHubTaskArtifactI} back to the `open*Tab` tool call that originally rendered its
 * clickable link card in the transcript. The `*_REFERENCED` kinds correspond to a tab the user opened.
 *
 * Workflows are special: the build/edit flow records a single WORKFLOW_CREATED/WORKFLOW_UPDATED row (server
 * dedups "one workflow -> one row" and preserves that kind even after a later openWorkflowTab), yet the agent
 * almost always opens the built workflow in the right panel — so a live card WAS shown. Those rows carry the
 * same projectId/projectWorkflowId metadata as WORKFLOW_REFERENCED, so we reconstruct them too; otherwise a
 * freshly built workflow's link card vanishes on reload. Other audit rows (row/column edits, memory) never
 * rendered a card and return null. The `args` shape mirrors what `openArtifactTab` in AiHubToolCallRenderer
 * expects, so the rehydrated card opens the same tab a live click would.
 */
function artifactToOpenToolCall(
    artifact: AiHubTaskArtifactI
): {args: Record<string, unknown>; toolName: string} | null {
    const metadata = parseArtifactMetadata(artifact.metadataJson);

    switch (artifact.kind) {
        case 'CODE_WORKFLOW_REFERENCED':
            // Same shape as CUSTOM_COMPONENT_REFERENCED below — artifactId IS the projectId. The `language`
            // openCodeWorkflowTab needs isn't stashed on the artifact (the recorder only stores projectId +
            // name), so this rehydrated tool-call args blob omits it rather than paying for a project fetch
            // per artifact on every task switch. Both openCustomComponentTab and openCodeWorkflowTab have
            // ARTIFACT_OPEN_META entries, so the card still renders as a clickable ArtifactLink, not plain
            // JSON — AiHubToolCallRenderer's openArtifactTab resolves the missing language lazily on click,
            // the same project-fetch-then-open flow as the sidebar's live quick-open
            // (AiHubTasksSidebar.openCodeWorkflowArtifact).
            return {
                args: {name: artifact.artifactName, projectId: artifact.artifactId},
                toolName: 'openCodeWorkflowTab',
            };
        case 'CUSTOM_COMPONENT_REFERENCED':
            return {
                args: {customComponentId: artifact.artifactId, name: artifact.artifactName},
                toolName: 'openCustomComponentTab',
            };
        case 'DATA_TABLE_REFERENCED':
            return {
                args: {dataTableId: artifact.artifactId, name: artifact.artifactName},
                toolName: 'openDataTableTab',
            };
        case 'FILE_REFERENCED':
            return {args: {fileId: artifact.artifactId, name: artifact.artifactName}, toolName: 'openFileTab'};
        case 'KB_REFERENCED':
            return {
                args: {knowledgeBaseId: artifact.artifactId, name: artifact.artifactName},
                toolName: 'openKnowledgeBaseTab',
            };
        case 'SKILL_REFERENCED':
            return {
                args: {name: artifact.artifactName, skillId: artifact.artifactId},
                toolName: 'openSkillTab',
            };
        case 'WORKFLOW_CREATED':
        case 'WORKFLOW_REFERENCED':
        case 'WORKFLOW_UPDATED':
            return {
                args: {
                    name: artifact.artifactName,
                    projectId: metadata['projectId'],
                    projectWorkflowId: Number(metadata['projectWorkflowId'] ?? 0),
                    workflowId: artifact.artifactId,
                },
                toolName: 'openWorkflowTab',
            };
        default:
            return null;
    }
}

/**
 * Rebuild the artifact link cards that streamed live but were lost on reload. The cards are tool-call UI
 * (fed by an ephemeral store), never persisted in chat memory — but the underlying open is durably recorded
 * as an `ai_hub_task_artifact` row. We synthesise one assistant message whose content is a `tool-call` part
 * per openable artifact; MessagePrimitive.Parts renders those via AiHubToolCallFallback -> ArtifactLink, the
 * same path as the live card. Appended after the text transcript (the cards were the last thing shown).
 */
function buildArtifactLinkMessages(artifacts: AiHubTaskArtifactI[]): ThreadMessageLike[] {
    const seen = new Set<string>();

    const parts = artifacts
        .map((artifact) => ({artifact, openCall: artifactToOpenToolCall(artifact)}))
        .filter(
            (
                entry
            ): entry is {artifact: AiHubTaskArtifactI; openCall: {args: Record<string, unknown>; toolName: string}} =>
                entry.openCall !== null
        )
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
            // Non-empty, stable id: the renderer drops orphan tool-call parts with an empty toolCallId, and a
            // per-artifact id keeps React keys stable across re-renders.
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
 * Returns whether the switch succeeded so callers can keep their dialog open on failure instead of closing
 * mid-error. The hook intentionally does NOT mutate command center/task state on failure — a partial overwrite
 * would leave the user typing into a phantom thread; reportMutationError surfaces the failure as a toast and the
 * caller should show a banner if they want a non-toast indication.
 *
 * Memoised with `useCallback` keyed only on `currentWorkspaceId` so callers (notably the URL <-> store sync
 * effects in AiHub.tsx) can put it in their dependency array without re-running on every render. A
 * fresh closure each render pulled the URL->store effect into a feedback loop that flashed the task
 * view on click and then reset back to the home view.
 */
export function useSwitchTask() {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const navigate = useNavigate();

    return useCallback(
        async (task: AiHubTaskI): Promise<boolean> => {
            // Switch the UI to the task IMMEDIATELY — set the thread id + select the task + navigate before
            // fetching the conversation history. Awaiting getTaskMessages first made the whole click block
            // on a network round-trip (instant when the messages were cached, several seconds on a cold
            // fetch — the intermittent lag the user saw). The thread renders empty for the brief moment
            // until the messages below resolve.
            aiHubStore.setState({
                messages: [],
                messagesLoading: true,
                taskId: task.threadId,
            });

            aiHubTasksStore.getState().setCurrentTaskId(task.id);

            // Navigate explicitly so the switch works from any CC page (not just /ai-hub
            // itself, which has its own store→URL sync effect). On the canonical /ai-hub
            // route this navigate is idempotent — AiHub.tsx's effect would fire next render
            // and find URL already matches the store, no-op. From /personal-agents or /workflow-chats
            // (which have no such effect), this is the only thing that flips the route to the
            // selected task. Without it, clicking a task row from those pages
            // updated the stores silently and left the user staring at the unchanged list.
            navigate(`/automation/ai-hub/tasks/${task.id}`);

            try {
                // Fetch history and artifacts together. Artifacts back the link-card reconstruction below;
                // a failure there must not break the (primary) message load, so it degrades to no cards.
                const [messages, artifacts] = await Promise.all([
                    getTaskMessages({
                        taskId: task.id,
                        workspaceId: currentWorkspaceId,
                    }),
                    getTaskArtifacts({taskId: task.id, workspaceId: currentWorkspaceId}).catch(
                        (): AiHubTaskArtifactI[] => []
                    ),
                ]);

                const mappedMessages: ThreadMessageLike[] = mapServerMessages(messages);

                // Chat memory persists only plain text, so the artifact link cards that streamed live are gone
                // on reload. Rebuild them from the durable artifact rows and append after the transcript.
                const artifactLinkMessages = buildArtifactLinkMessages(artifacts);

                // Apply only if the user is still on this task — a slower fetch for task A must not clobber
                // the thread (or clear the loading flag) after the user has already clicked task B.
                if (aiHubStore.getState().taskId === task.threadId) {
                    aiHubStore.setState({
                        messages: [...mappedMessages, ...artifactLinkMessages],
                        messagesLoading: false,
                    });
                }

                return true;
            } catch (error) {
                if (aiHubStore.getState().taskId === task.threadId) {
                    aiHubStore.setState({messagesLoading: false});
                }

                // Without surfacing this, the user keeps typing into what they think is the new task
                // but is in fact still the previous one. Routing through reportMutationError keeps the toast
                // wording consistent with the other task mutations in useTasks.
                reportMutationError('Switch task', error instanceof Error ? error : new Error(String(error)));

                return false;
            }
        },
        [currentWorkspaceId, navigate]
    );
}
