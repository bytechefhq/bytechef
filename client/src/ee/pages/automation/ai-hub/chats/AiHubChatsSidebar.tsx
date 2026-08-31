import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Input} from '@/components/ui/input';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    getChatDisplayTitle,
    isChannelAgentChat,
    isWebhookBridgedChat,
} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {
    useAiHubChatsQuery,
    useDeleteAiHubChatMutation,
    usePatchAiHubChatMutation,
} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {useLiveWorkflowLabel} from '@/ee/pages/automation/ai-hub/chats/hooks/useLiveWorkflowLabel';
import {useSwitchChat} from '@/ee/pages/automation/ai-hub/chats/hooks/useSwitchChat';
import {aiHubChatsStore, useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {probeInFlightStatus} from '@/ee/pages/automation/ai-hub/runtime-providers/inFlightRunClient';
import {
    aiHubRunStateStore,
    isChatRunning,
} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useCancelAiHubRunMutation, useCancelWorkflowChatTurnMutation} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    ArchiveIcon,
    ArchiveRestoreIcon,
    BlocksIcon,
    BotIcon,
    CalendarClockIcon,
    ChevronDownIcon,
    ChevronLeftIcon,
    HexagonIcon,
    LinkIcon,
    Loader2Icon,
    MessageCircleQuestionIcon,
    MessageSquareIcon,
    MessageSquarePlusIcon,
    MoreVerticalIcon,
    NotebookIcon,
    PencilIcon,
    RadioTowerIcon,
    RotateCcwIcon,
    Trash2Icon,
    WorkflowIcon,
} from 'lucide-react';
import {type MouseEvent, useEffect, useMemo, useRef, useState} from 'react';
import {Link, useLocation, useNavigate} from 'react-router-dom';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

import {AiHubChatI, generateAiHubChatTitle} from './api/chats.api';

export const CHATS_PAGE_SIZE = 25;

export function getChatsPage<T>(chats: T[], visibleCount: number): {hiddenCount: number; visibleChats: T[]} {
    return {
        hiddenCount: Math.max(0, chats.length - visibleCount),
        visibleChats: chats.slice(0, visibleCount),
    };
}

/*
 * Single continuous column mirroring the settled chat list (flat rows, `gap-0.5`) — the list has no
 * visual grouping, so a grouped skeleton would read as two separate loading boxes on every reload.
 */
const ChatsSidebarSkeleton = () => (
    <div className="flex flex-col gap-0.5 px-2">
        {Array.from({length: 8}).map((_, itemIndex) => (
            <Skeleton className="h-8 w-full" key={itemIndex} />
        ))}
    </div>
);

/**
 * Cancel a chat's in-flight run when it is deleted mid-stream, so the server stops producing events
 * instead of leaving the run in InFlightAiHubRunRegistry until its TTL (where it keeps consuming model
 * tokens for a chat the user just removed). Mirrors {@link AiHubChatComposer}'s handleCancelTurn but is
 * keyed off the deleted chat's thread id, because a delete can target a background (non-focused) chat.
 *
 * "Streaming" spans two signals: the focused chat sets runningByChat via RUN_STARTED, while a background
 * chat's visible pulse comes from the probe-driven 'running' activity state. Both are covered. The server
 * cancel is idempotent, so a false positive is harmless. No-op when the chat isn't streaming.
 */
export function cancelChatRunIfStreaming(
    chat: Pick<AiHubChatI, 'id' | 'kind' | 'threadId'>,
    workspaceId: number | undefined,
    cancel: {
        cancelAiHubRun: (variables: {id: string; runId: string | undefined; workspaceId: string}) => void;
        cancelWorkflowChatTurn: (variables: {id: string; workspaceId: string}) => void;
    }
): void {
    const runState = aiHubRunStateStore.getState();

    const isStreaming =
        isChatRunning(runState, chat.threadId) || aiHubChatsStore.getState().chatActivity[chat.threadId] === 'running';

    if (!isStreaming || workspaceId == null) {
        return;
    }

    if (isWebhookBridgedChat(chat.kind)) {
        cancel.cancelWorkflowChatTurn({id: String(chat.id), workspaceId: String(workspaceId)});
    } else {
        cancel.cancelAiHubRun({
            id: String(chat.id),
            runId: runState.runIdByChat[chat.threadId],
            workspaceId: String(workspaceId),
        });
    }

    runState.setChatRunning(chat.threadId, false);

    aiHubChatsStore.getState().clearActivityState(chat.threadId);
}

export interface ProbedChatActivityDecisionI {
    clearActivity?: boolean;
    clearRunningFlag?: boolean;
    setActivityRunning?: boolean;
}

/**
 * Pure reconciliation for one chat row in the sidebar's in-flight probe. Returns which store mutations to apply.
 *
 * The FOCUSED chat's activity is owned by the runtime's own lifecycle hooks (onNew / onRunFinished / onCancel):
 * the probe must neither SET nor CLEAR it out from under them. In particular, NOT re-setting 'running' for the
 * focused thread is what fixes the stale-spinner-after-Stop bug — after the user cancels, the async server-cancel
 * hasn't landed yet, so a probe firing in that window still reports in-flight and would otherwise re-pulse the dot
 * the cancel just cleared. Background chats have no runtime hooks, so the probe is authoritative for them.
 */
export function reconcileProbedChatActivity({
    currentActivity,
    isFocused,
    isInFlight,
    isRunningByChat,
}: {
    currentActivity: string | undefined;
    isFocused: boolean;
    isInFlight: boolean;
    isRunningByChat: boolean;
}): ProbedChatActivityDecisionI {
    if (isInFlight) {
        if (currentActivity == null && !isFocused) {
            return {setActivityRunning: true};
        }

        return {};
    }

    if (currentActivity === 'paused') {
        return {};
    }

    if (isFocused) {
        if (currentActivity === 'running' && !isRunningByChat) {
            return {clearActivity: true};
        }

        return {};
    }

    const decision: ProbedChatActivityDecisionI = {};

    if (isRunningByChat) {
        decision.clearRunningFlag = true;
    }

    if (currentActivity === 'running') {
        decision.clearActivity = true;
    }

    return decision;
}

interface ChatItemProps {
    chat: AiHubChatI;
    isCurrent: boolean;
    onArchive: () => void;
    onDelete: () => void;
    onRename: () => void;
    onSelect: () => void;
    onUnarchive: () => void;
    workspaceId: number;
}

const ChatItem = ({
    chat,
    isCurrent,
    onArchive,
    onDelete,
    onRename,
    onSelect,
    onUnarchive,
    workspaceId,
}: ChatItemProps) => {
    const queryClient = useQueryClient();
    const isArchived = chat.status === 'ARCHIVED';
    // Channel-born agent chats (Slack, a schedule, …) share kind=AGENT_CHAT with composer-started ones but
    // must not read as the user's own conversations — a busy channel can produce dozens of them. Distinguish
    // with a different icon/tooltip and a generic fallback title (getChatDisplayTitle) rather than the
    // "New Chat" placeholder an unstarted draft uses.
    const isChannelBorn = isChannelAgentChat(chat);

    // Subscribe specifically to this chat's title-generation failure flag so a retry icon appears
    // on the row when the most recent automatic title-generation attempt failed. Without this, the toast
    // shown at failure time is the only signal — once dismissed, the user has no recovery path.
    const titleGenerationError = useAiHubChatsStore((state) => state.titleGenerationFailures[chat.id]);

    // Surface the LIVE workflow label so a stale chat title (set on first creation as
    // "${projectName} — ${workflowLabel}") doesn't hide the fact that the workflow has since been renamed.
    // We only enable the query for kind=WORKFLOW_CHAT rows so standard-only sidebars don't pay the workflow
    // lookup cost. The query is cached workspace-wide so multiple workflow-chat rows share one fetch.
    const liveWorkflowLabel = useLiveWorkflowLabel(chat);

    // In-flight signal for this row: 'running' = the chat is mid-turn, 'paused' = the workflow is
    // paused at ask_user_question waiting for the user. Same store as titleGenerationError so subscriptions
    // re-render the row independently — a paused chat buried at the bottom of the sidebar still
    // updates without re-rendering all the others.
    const activityState = useAiHubChatsStore((state) => state.chatActivity[chat.threadId]);
    const [isRetryingTitle, setIsRetryingTitle] = useState(false);

    const handleRetryTitleGeneration = async (event: MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();

        if (isRetryingTitle) {
            return;
        }

        setIsRetryingTitle(true);

        try {
            await generateAiHubChatTitle({chatId: chat.id, workspaceId});

            aiHubChatsStore.getState().clearTitleGenerationFailure(chat.id);

            await queryClient.invalidateQueries({
                queryKey: ['aiHub', 'chats'],
            });
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);

            // Preserve the failure flag so the retry icon stays visible and the user can try again later.
            aiHubChatsStore.getState().markTitleGenerationFailed(chat.id, errorMessage);

            toast.error(`Failed to generate chat title: ${errorMessage}`);
        } finally {
            setIsRetryingTitle(false);
        }
    };

    return (
        <div>
            <div
                className={twMerge(
                    'group flex w-full items-center justify-between rounded-md px-2 py-1.5 text-sm hover:bg-accent',
                    isCurrent && 'bg-accent'
                )}
            >
                <button
                    className="flex flex-1 items-center gap-1.5 truncate text-left"
                    onClick={onSelect}
                    type="button"
                >
                    {chat.kind === 'AGENT_CHAT' && isChannelBorn && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <RadioTowerIcon
                                    aria-label="Agent channel conversation"
                                    className="size-3.5 shrink-0 text-muted-foreground"
                                />
                            </TooltipTrigger>

                            <TooltipContent side="right">
                                Agent conversation — recorded from the agent&apos;s channel (Slack, a schedule, …), not
                                started here. Only you can see it, since you created the agent.
                            </TooltipContent>
                        </Tooltip>
                    )}

                    {chat.kind === 'AGENT_CHAT' && !isChannelBorn && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <BotIcon
                                    aria-label="Agent chat"
                                    className="size-3.5 shrink-0 text-muted-foreground"
                                    role="img"
                                />
                            </TooltipTrigger>

                            <TooltipContent side="right">
                                Agent chat — bound to an AI Agent. Messages are forwarded to the agent&apos;s workflow
                                instead of the AI Hub&apos;s own LLM.
                            </TooltipContent>
                        </Tooltip>
                    )}

                    {chat.kind === 'WORKFLOW_CHAT' && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <WorkflowIcon
                                    aria-label="Workflow chat"
                                    className="size-3.5 shrink-0 text-muted-foreground"
                                    role="img"
                                />
                            </TooltipTrigger>

                            <TooltipContent side="right">
                                Workflow chat — bound to a workflow execution. Messages are forwarded to the workflow's
                                webhook trigger instead of an LLM.
                                {liveWorkflowLabel && liveWorkflowLabel !== chat.title && (
                                    <div className="mt-1 text-xs opacity-90">
                                        Current workflow label: <span className="font-medium">{liveWorkflowLabel}</span>
                                    </div>
                                )}
                            </TooltipContent>
                        </Tooltip>
                    )}

                    {/*
                     * Default-kind icon: a plain MessageSquare for the STANDARD chats so every
                     * row is visually anchored. Without this, Workflow Chat and Agent Chat rows had icons
                     * but standard rows started with text — the eye lost its alignment column when scanning the
                     * list. Every kind that owns an icon above is excluded explicitly rather than by testing for
                     * STANDARD, so a chat whose kind this client doesn't know still renders with an icon; the
                     * toChat mapper already funnels unrecognised server kinds to STANDARD. No tooltip: STANDARD
                     * is the implicit default kind, and a tooltip on every standard row would be noise.
                     */}

                    {chat.kind === 'STANDARD' && (
                        <MessageSquareIcon aria-label="Chat" className="size-3.5 shrink-0 text-muted-foreground" />
                    )}

                    <span className={twMerge('truncate', isCurrent && 'font-semibold')}>
                        {getChatDisplayTitle(chat)}
                    </span>

                    {/*
                     * Activity indicator. 'running' shows a spinning loader so the user knows their last turn
                     * is still in flight even if they switched away from this chat; 'paused' shows a
                     * question-mark badge meaning "this chat needs your answer to continue." Both signals are
                     * driven from AiHubRuntimeProvider's lifecycle event handlers.
                     */}

                    {activityState === 'running' && (
                        <Loader2Icon
                            aria-label="Chat is running"
                            className="size-3 shrink-0 animate-spin text-muted-foreground"
                        />
                    )}

                    {activityState === 'paused' && (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <MessageCircleQuestionIcon
                                    aria-label="Chat is paused waiting for your answer"
                                    className="size-3.5 shrink-0 text-content-warning-primary"
                                    role="img"
                                />
                            </TooltipTrigger>

                            <TooltipContent side="right">
                                Workflow paused for your answer. Reply to resume the workflow run.
                            </TooltipContent>
                        </Tooltip>
                    )}
                </button>

                <div className="ml-1 flex shrink-0 items-center gap-0.5">
                    {titleGenerationError && (
                        <button
                            aria-label="Retry title generation"
                            className="rounded p-0.5 text-content-warning-primary hover:bg-muted"
                            data-testid="retry-title-generation"
                            disabled={isRetryingTitle}
                            onClick={handleRetryTitleGeneration}
                            title={`Title generation failed: ${titleGenerationError}. Click to retry.`}
                            type="button"
                        >
                            <RotateCcwIcon className={twMerge('size-3.5', isRetryingTitle && 'animate-spin')} />
                        </button>
                    )}

                    {/*
                     * Hover-only action buttons. Switched from `opacity-0 group-hover:opacity-100` to
                     * `hidden group-hover:flex` so the buttons don't reserve layout space when invisible —
                     * earlier, the chevron + more-horizontal kept ~36px of room on every row, truncating
                     * the chat title way before the panel's right edge. With `hidden`, the title
                     * stretches to the full row width when the user isn't hovering; on hover the buttons
                     * appear and re-truncate the title (acceptable since the user is mousing over).
                     * `focus-within:flex` keeps keyboard navigation working without a hover.
                     */}

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <button
                                aria-label="Chat actions"
                                className="hidden rounded p-0.5 group-focus-within:flex group-hover:flex hover:bg-muted focus:flex data-[state=open]:flex"
                                data-testid="chat-actions-trigger"
                                onClick={(event) => event.stopPropagation()}
                                type="button"
                            >
                                <MoreVerticalIcon className="size-4" />
                            </button>
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={onRename}>
                                <PencilIcon />
                                Rename
                            </DropdownMenuItem>

                            {isArchived ? (
                                <DropdownMenuItem onClick={onUnarchive}>
                                    <ArchiveRestoreIcon />
                                    Unarchive
                                </DropdownMenuItem>
                            ) : (
                                <DropdownMenuItem onClick={onArchive}>
                                    <ArchiveIcon />
                                    Archive
                                </DropdownMenuItem>
                            )}

                            <DropdownMenuItem onClick={onDelete} variant="destructive">
                                <Trash2Icon />
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>
        </div>
    );
};

interface RenameDialogStateI {
    chatId: number;
    currentTitle: string;
}

interface DeleteDialogStateI {
    targetChat: AiHubChatI;
}

const AiHubChatsSidebar = () => {
    const [renameState, setRenameState] = useState<RenameDialogStateI | null>(null);
    const [renameInputValue, setRenameInputValue] = useState('');
    const [deleteDialogState, setDeleteDialogState] = useState<DeleteDialogStateI | null>(null);
    const [visibleCount, setVisibleCount] = useState(CHATS_PAGE_SIZE);
    const renameInputRef = useRef<HTMLInputElement>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const activeFilter = useAiHubChatsStore((state) => state.activeFilter);
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);
    const searchTerm = useAiHubChatsStore((state) => state.searchTerm);
    const setActiveFilter = useAiHubChatsStore((state) => state.setActiveFilter);
    const setSearchTerm = useAiHubChatsStore((state) => state.setSearchTerm);

    const switchChat = useSwitchChat();

    const {data: chats, isLoading} = useAiHubChatsQuery(
        currentWorkspaceId,
        currentEnvironmentId,
        activeFilter === 'ACTIVE' ? 'ACTIVE' : 'ARCHIVED'
    );

    const patchChatMutation = usePatchAiHubChatMutation();
    const deleteChatMutation = useDeleteAiHubChatMutation();
    const cancelAiHubRunMutation = useCancelAiHubRunMutation();
    const cancelWorkflowChatTurnMutation = useCancelWorkflowChatTurnMutation();

    // Hydrate the sidebar's per-chat running pulse from the server. Activity state is otherwise driven only
    // by the runtime provider's lifecycle hooks (RUN_STARTED → 'running' / RUN_FINISHED → cleared), which
    // means a page refresh blows the whole map away even though server-side runs are still streaming. Probing
    // on chats list change rehydrates the dot for every chat the user has actively running, not just the
    // focused one — which is what makes the "switch between concurrent streams" UX work: the user sees
    // multiple pulses, can switch, and the runtime provider's own attach effect resumes whichever chat they
    // land on.
    //
    // The probe is also responsible for CLEARING stale 'running' indicators on background chats:
    // {@link AiHubRuntimeProvider}'s {@code onRunFinishedEvent} clears state only for the chat whose
    // {@code /attach} EventSource it owns (the focused one). Background in-flight chats that finish
    // server-side have no other code path to clear their pulse, so without probe-driven clearing the
    // indicator stuck forever. Two guards keep the clear safe:
    //
    //   1. Preserve {@code 'paused'} — {@link AiHubRuntimeProvider}'s {@code onCustomEvent} sets it on
    //      workflow chats that hit {@code ask_user_question}, and the runtime intentionally keeps it
    //      across the immediately-following RUN_FINISHED. A probe issued after that RUN_FINISHED would
    //      otherwise wipe the user-actionable "needs your answer" indicator.
    //
    //   2. Skip clearing when the local runtime claims the chat via {@link aiHubRunStateStore.runningByChat}.
    //      Between user-clicks-send and agent-POST-registers-run-on-the-server there's a narrow window where
    //      a probe issued moments earlier could come back reporting {@code false}; deferring to the
    //      runtime's RUN_FINISHED / onCancel / onClose clear paths avoids clobbering a turn the local
    //      client is actively driving.
    //
    // Also re-runs on a 20-second interval so background chats get cleaned up even when the chats list
    // query is otherwise idle (the list has a 30s staleTime and no refetchInterval).
    useEffect(() => {
        if (!chats || chats.length === 0) {
            return;
        }

        const visibleThreadIds = chats.map((chat) => chat.threadId);

        let cancelled = false;

        const runProbe = () => {
            void probeInFlightStatus(visibleThreadIds).then((statusByThreadId) => {
                if (cancelled) {
                    return;
                }

                const chatsState = aiHubChatsStore.getState();
                const runState = aiHubRunStateStore.getState();

                const focusedThreadId = chats.find((chat) => chat.id === currentChatId)?.threadId;

                visibleThreadIds.forEach((threadId) => {
                    const decision = reconcileProbedChatActivity({
                        currentActivity: chatsState.chatActivity[threadId],
                        isFocused: threadId === focusedThreadId,
                        isInFlight: statusByThreadId[threadId] ?? false,
                        isRunningByChat: Boolean(runState.runningByChat[threadId]),
                    });

                    if (decision.setActivityRunning) {
                        chatsState.setActivityState(threadId, 'running');
                    }

                    if (decision.clearRunningFlag) {
                        runState.setChatRunning(threadId, false);
                    }

                    if (decision.clearActivity) {
                        chatsState.clearActivityState(threadId);
                    }
                });
            });
        };

        runProbe();

        const intervalHandle = setInterval(runProbe, 20_000);

        return () => {
            cancelled = true;

            clearInterval(intervalHandle);
        };
    }, [chats, currentChatId]);

    const filteredChats = useMemo(() => {
        if (!chats) {
            return [];
        }

        const lowerSearch = searchTerm.toLowerCase();

        if (!lowerSearch) {
            return chats;
        }

        return chats.filter(
            (chat) =>
                (chat.title ?? '').toLowerCase().includes(lowerSearch) ||
                (chat.lastPreview ?? '').toLowerCase().includes(lowerSearch)
        );
    }, [chats, searchTerm]);

    const {hiddenCount, visibleChats} = useMemo(
        () => getChatsPage(filteredChats, visibleCount),
        [filteredChats, visibleCount]
    );

    useEffect(() => {
        setVisibleCount(CHATS_PAGE_SIZE);
    }, [searchTerm, activeFilter]);

    const handleSelectChat = (chat: AiHubChatI) => {
        switchChat(chat);
    };

    const handleRename = (chat: AiHubChatI) => {
        setRenameState({chatId: chat.id, currentTitle: chat.title ?? ''});
        setRenameInputValue(chat.title ?? '');

        setTimeout(() => {
            renameInputRef.current?.focus();
            renameInputRef.current?.select();
        }, 0);
    };

    const handleRenameSubmit = () => {
        if (!renameState) {
            return;
        }

        const trimmedTitle = renameInputValue.trim();

        if (trimmedTitle && trimmedTitle !== renameState.currentTitle) {
            patchChatMutation.mutate({
                chatId: renameState.chatId,
                patch: {title: trimmedTitle},
                workspaceId: currentWorkspaceId,
            });
        }

        setRenameState(null);
    };

    const handleArchive = (chat: AiHubChatI) => {
        patchChatMutation.mutate({
            chatId: chat.id,
            patch: {status: 'ARCHIVED'},
            workspaceId: currentWorkspaceId,
        });

        if (currentChatId === chat.id) {
            aiHubChatsStore.getState().setCurrentChatId(undefined);
        }
    };

    const handleUnarchive = (chat: AiHubChatI) => {
        patchChatMutation.mutate({
            chatId: chat.id,
            patch: {status: 'ACTIVE'},
            workspaceId: currentWorkspaceId,
        });
    };

    const handleDelete = (chat: AiHubChatI) => {
        setDeleteDialogState({targetChat: chat});
    };

    const handleDeleteDialogConfirm = () => {
        if (!deleteDialogState) {
            return;
        }

        const chat = deleteDialogState.targetChat;

        // Stop the stream before removing the chat: cancel its in-flight run (server-side) so it doesn't
        // keep running for a chat the user just deleted. Clearing currentChatId below tears down the
        // focused runtime's SSE for the active chat; this handles the background chats too.
        cancelChatRunIfStreaming(chat, currentWorkspaceId, {
            cancelAiHubRun: (variables) => cancelAiHubRunMutation.mutate(variables),
            cancelWorkflowChatTurn: (variables) => cancelWorkflowChatTurnMutation.mutate(variables),
        });

        deleteChatMutation.mutate({
            chatId: chat.id,
            workspaceId: currentWorkspaceId,
        });

        if (currentChatId === chat.id) {
            // Deleting the chat the user is currently viewing: reset to the home view and redirect so they
            // don't sit on a stale /chats/<id> route for a chat that no longer exists. Mirrors AiHub.tsx's
            // home-reset (clear messages + fresh thread id); the explicit navigate guarantees the redirect.
            aiHubChatsStore.getState().setCurrentChatId(undefined);
            aiHubStore.getState().resetMessages();
            aiHubStore.getState().generateChatId();

            navigate('/automation/ai-hub');
        }

        setDeleteDialogState(null);
    };

    const isViewingArchived = activeFilter === 'ARCHIVED';

    const {pathname} = useLocation();
    const navigate = useNavigate();

    // Active-state predicates for the top-level menu items.
    //
    // - New Chat matches ONLY the bare /automation/ai-hub landing. When the user is on a specific
    //   /automation/ai-hub/chats/<id> page, that chat's row in the list below already shows the active
    //   highlight — also lighting up "New Chat" would imply two simultaneous selections and visually
    //   conflate "start a fresh chat" with "currently on a chat".
    // - Scheduled, Memories, Connectors, and Skills use exact prefix matches against their
    //   canonical routes.
    const isOnNewChat = pathname === '/automation/ai-hub';
    const isOnMemories = pathname.startsWith('/automation/ai/memories');
    const isOnConnectors = pathname.startsWith('/automation/settings/ai-hub/connectors');
    const isOnScheduled = pathname.startsWith('/automation/ai-hub/scheduled');
    const isOnSkills = pathname.startsWith('/automation/settings/ai/skills');

    const isOnMoreTarget = isOnMemories || isOnConnectors || isOnSkills;

    // `bg-accent` alone is the same shade as `hover:bg-accent`, so an "active" menu item is visually
    // indistinguishable from a hovered one — the user reads the row as not-selected. Mirror the active-chat
    // row treatment (font-semibold + foreground text) so the active state has a real visual delta even with the
    // subtle background tint. `text-foreground` on active anchors the label at full contrast; `text-muted-foreground`
    // on inactive rows lets the active label "step forward" rather than relying on the background alone.
    const menuItemClass = (isActive: boolean) =>
        twMerge(
            'flex items-center gap-2 rounded-md px-2 py-1.5 text-sm font-medium text-muted-foreground hover:bg-accent hover:text-foreground',
            isActive && 'bg-accent font-semibold text-foreground'
        );

    return (
        <div className="flex flex-col gap-2 px-2 pb-2">
            {/*
             * Top-level menu block: New Chat / More. Aligned at px-2 so labels land at
             * 8 (body p-2) + 8 (item px-2) = 16px from the sidebar edge — matches the px-4 padding on the
             * "AI Hub" Header above.
             *
             * Workflow Chats used to sit here as a third link to its own full-page route. That page is gone:
             * the composer's provider popup now carries a Workflow Chats cascade (and an Agent Chats one)
             * and is the single launcher for every kind of chat, so a separate destination for one of them
             * was a second way to do the same thing.
             *
             * Scheduled is back as a top-level row. Scheduling itself lives on the AI Agent, so its page is
             * a roll-up of every agent's schedule rather than an editor — but it is a destination the user
             * reaches for as readily as a new chat, which is what earns it a row instead of a menu entry.
             * Links here, not buttons setting state. Each row applies a `bg-accent` highlight when its route
             * is active so the user can see at a glance which destination they're on.
             */}

            {/*
             * Tight `gap-0.5` (2px) between the menu rows. The outer wrapper still uses `gap-2`
             * to keep clear visual separation between this menu block, the Chats title row,
             * the search input, and the chat list — those are different sections, not
             * siblings of the menu items.
             */}

            {/*
             * Icons match the rest of the AI Hub surface for visual continuity:
             *   - MessageSquarePlus signals "start a new chat" — same metaphor used in mainstream chat UIs.
             *   - CalendarClock signals "runs on a clock" for Scheduled.
             * `text-muted-foreground` keeps the icons quiet so the label remains the primary anchor.
             */}

            <div className="flex flex-col gap-0.5">
                <Link className={menuItemClass(isOnNewChat)} to="/automation/ai-hub">
                    <MessageSquarePlusIcon className="size-4 shrink-0 text-muted-foreground" />

                    <span>New Chat</span>
                </Link>

                {/* Scheduled sits beside New Chat rather than in "More": the two are the hub's ways of
                    getting an agent to run — one by asking now, one by asking on a clock — whereas the
                    "More" entries are resources a chat draws on. */}

                <Link className={menuItemClass(isOnScheduled)} to="/automation/ai-hub/scheduled">
                    <CalendarClockIcon className="size-4 shrink-0 text-muted-foreground" />

                    <span>Scheduled</span>
                </Link>

                {/*
                 * "More" — a popup menu of AI Hub-wide resources that aren't scoped to a single chat. It opens
                 * as an overlay ANCHORED INSIDE the sidebar (below the trigger, matched to its width) rather
                 * than flying out to the right: the chat list keeps its position, and the menu still reads as
                 * part of the sidebar rather than as a panel over the page. The trigger stays highlighted
                 * while the user is on one of the three target pages, and the items are plain links to their
                 * canonical destinations.
                 */}

                <DropdownMenu>
                    <DropdownMenuTrigger className={twMerge(menuItemClass(isOnMoreTarget), 'w-full justify-between')}>
                        <span className="flex items-center gap-2">
                            <BlocksIcon className="size-4 shrink-0 text-muted-foreground" />

                            <span>More</span>
                        </span>

                        <ChevronDownIcon className="size-4 shrink-0 text-muted-foreground" />
                    </DropdownMenuTrigger>

                    {/* Width matched to the trigger and offset by a hair so the panel sits within the rail
                        instead of overhanging it. */}

                    <DropdownMenuContent
                        align="start"
                        className="w-(--radix-dropdown-menu-trigger-width)"
                        side="bottom"
                        sideOffset={2}
                    >
                        <DropdownMenuItem asChild>
                            <Link to="/automation/settings/ai-hub/connectors">
                                <LinkIcon />
                                Connectors
                            </Link>
                        </DropdownMenuItem>

                        <DropdownMenuItem asChild>
                            <Link to="/automation/ai/memories">
                                <NotebookIcon />
                                Memories
                            </Link>
                        </DropdownMenuItem>

                        <DropdownMenuItem asChild>
                            <Link to="/automation/settings/ai/skills">
                                <HexagonIcon />
                                Skills
                            </Link>
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {/*
             * Title row pinned at the top: page-anchor "Chats" label on the left, Archive toggle
             * affordance on the right. The Archive toggle flips the same `activeFilter` store value the
             * bottom toggle used to drive — when in ARCHIVED view the icon swaps to a "back to active"
             * affordance so the user always has a clear way back.
             *
             * Memories used to live here as a small icon; it's been promoted to an entry in the "More"
             * menu above (alongside Connectors and Skills). One canonical entry point per
             * destination keeps the sidebar uncluttered and avoids the "two ways to do the same thing"
             * trap where users wonder whether the small icon and the top-level item differ.
             */}

            <div className="flex items-center justify-between gap-2 px-2">
                <h4 className="text-sm font-semibold text-foreground">Chats</h4>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <button
                            aria-label={isViewingArchived ? 'Back to active chats' : 'Archive'}
                            aria-pressed={isViewingArchived}
                            className={twMerge(
                                'inline-flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground',
                                isViewingArchived && 'bg-accent text-foreground'
                            )}
                            onClick={() => setActiveFilter(isViewingArchived ? 'ACTIVE' : 'ARCHIVED')}
                            type="button"
                        >
                            {isViewingArchived ? (
                                <ChevronLeftIcon className="size-3.5" />
                            ) : (
                                <ArchiveIcon className="size-3.5" />
                            )}
                        </button>
                    </TooltipTrigger>

                    <TooltipContent>{isViewingArchived ? 'Back to active' : 'Archive'}</TooltipContent>
                </Tooltip>
            </div>

            {/*
             * Search-only row. The previous "+" quick-create button next to the input is removed — the
             * "New Chat" entry in the top menu block is the canonical create entry point, and the
             * duplicate "+" here read as visual noise once the menu items moved up to the top of the sidebar.
             *
             * px-2 wrapper so the input aligns at 16px from the sidebar edge — same as the Header title
             * and the menu items above. Without it, the Input touches the p-2 body edge at 8px.
             */}

            <div className="px-2">
                <Input
                    onChange={(event) => setSearchTerm(event.target.value)}
                    placeholder="Search chats..."
                    value={searchTerm}
                />
            </div>

            {isLoading ? (
                <ChatsSidebarSkeleton />
            ) : filteredChats.length === 0 ? (
                <div className="px-2 py-4">
                    <span className="px-3 text-xs text-muted-foreground">
                        {isViewingArchived ? 'No archived chats yet.' : 'No chats yet.'}
                    </span>
                </div>
            ) : (
                <div className="flex flex-col gap-0.5">
                    {visibleChats.map((chat) =>
                        renameState?.chatId === chat.id ? (
                            <div className="px-1" key={chat.id}>
                                <Input
                                    className="h-8 text-sm"
                                    onBlur={handleRenameSubmit}
                                    onChange={(event) => setRenameInputValue(event.target.value)}
                                    onKeyDown={(event) => {
                                        if (event.key === 'Enter') {
                                            handleRenameSubmit();
                                        } else if (event.key === 'Escape') {
                                            setRenameState(null);
                                        }
                                    }}
                                    ref={renameInputRef}
                                    value={renameInputValue}
                                />
                            </div>
                        ) : (
                            <ChatItem
                                chat={chat}
                                isCurrent={currentChatId === chat.id}
                                key={chat.id}
                                onArchive={() => handleArchive(chat)}
                                onDelete={() => handleDelete(chat)}
                                onRename={() => handleRename(chat)}
                                onSelect={() => handleSelectChat(chat)}
                                onUnarchive={() => handleUnarchive(chat)}
                                workspaceId={currentWorkspaceId}
                            />
                        )
                    )}

                    {hiddenCount > 0 && (
                        <button
                            className="rounded-md px-2 py-1.5 text-left text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                            onClick={() => setVisibleCount((previousCount) => previousCount + CHATS_PAGE_SIZE)}
                            type="button"
                        >
                            {`Show ${Math.min(hiddenCount, CHATS_PAGE_SIZE)} more`}
                        </button>
                    )}
                </div>
            )}

            <AlertDialog
                onOpenChange={(nextOpen) => {
                    if (!nextOpen) {
                        setDeleteDialogState(null);
                    }
                }}
                open={deleteDialogState !== null}
            >
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Delete this chat?</AlertDialogTitle>

                        <AlertDialogDescription>
                            {deleteDialogState &&
                                `"${getChatDisplayTitle(deleteDialogState.targetChat)}" will be permanently deleted. This cannot be undone.`}
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>

                        <AlertDialogAction onClick={handleDeleteDialogConfirm}>Delete</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    );
};

export default AiHubChatsSidebar;
