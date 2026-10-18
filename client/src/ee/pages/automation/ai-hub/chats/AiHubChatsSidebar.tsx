import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Input} from '@/components/ui/input';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useAiHubChatActions} from '@/ee/pages/automation/ai-hub/chats/hooks/useAiHubChatActions';
import {AiHubChatsKeys, useAiHubChatsQuery} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {useSwitchChat} from '@/ee/pages/automation/ai-hub/chats/hooks/useSwitchChat';
import {isWorkspaceIdKnown} from '@/ee/pages/automation/ai-hub/chats/isWorkspaceIdKnown';
import {aiHubChatsStore, useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    ArchiveIcon,
    ArchiveRestoreIcon,
    ChevronLeftIcon,
    Loader2Icon,
    MessageCircleQuestionIcon,
    MessageSquareIcon,
    MessageSquarePlusIcon,
    MoreVerticalIcon,
    PencilIcon,
    RotateCcwIcon,
    Trash2Icon,
} from 'lucide-react';
import {type MouseEvent, useEffect, useMemo, useState} from 'react';
import {Link, useLocation} from 'react-router-dom';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

import AiHubChatActionDialogs from './AiHubChatActionDialogs';
import {AiHubChatI, generateAiHubChatTitle, getChatDisplayTitle} from './api/chats.api';

export const CHATS_PAGE_SIZE = 25;

export function getChatsPage<T>(chats: T[], visibleCount: number): {hiddenCount: number; visibleChats: T[]} {
    return {
        hiddenCount: Math.max(0, chats.length - visibleCount),
        visibleChats: chats.slice(0, visibleCount),
    };
}

const ChatsSidebarSkeleton = () => (
    <div className="flex flex-col gap-0.5">
        {Array.from({length: 8}).map((_, itemIndex) => (
            <Skeleton className="h-8 w-full" key={itemIndex} />
        ))}
    </div>
);

const matchesSearch = (chat: AiHubChatI, lowerSearch: string) =>
    (chat.title ?? '').toLowerCase().includes(lowerSearch) ||
    (chat.lastPreview ?? '').toLowerCase().includes(lowerSearch);

interface ChatItemProps {
    chat: AiHubChatI;
    isCurrent: boolean;
    onArchive: () => void;
    onDelete: () => void;
    onRename: () => void;
    onSelect: () => void;
    onUnarchive: () => void;
    workspaceId: number | undefined;
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
    const [isRetryingTitle, setIsRetryingTitle] = useState(false);

    const activityState = useAiHubChatsStore((state) => state.chatActivity[chat.threadId]);
    const titleGenerationError = useAiHubChatsStore((state) => state.titleGenerationFailures[chat.id]);

    const queryClient = useQueryClient();

    const isArchived = chat.status === 'ARCHIVED';

    const handleRetryTitleGeneration = async (event: MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();

        if (isRetryingTitle || !isWorkspaceIdKnown(workspaceId)) {
            return;
        }

        setIsRetryingTitle(true);

        try {
            await generateAiHubChatTitle({chatId: chat.id, workspaceId});

            aiHubChatsStore.getState().clearTitleGenerationFailure(chat.id);

            await queryClient.invalidateQueries({queryKey: AiHubChatsKeys.all});
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : String(error);

            aiHubChatsStore.getState().markTitleGenerationFailed(chat.id, errorMessage);

            toast.error(`Failed to generate chat title: ${errorMessage}`);
        } finally {
            setIsRetryingTitle(false);
        }
    };

    return (
        <div
            className={twMerge(
                'group flex w-full items-center justify-between rounded-md py-1.5 pl-2 text-sm text-sidebar-foreground hover:bg-accent hover:text-sidebar-accent-foreground',
                isCurrent && 'bg-accent text-sidebar-accent-foreground'
            )}
        >
            <button className="flex flex-1 items-center gap-1.5 truncate text-left" onClick={onSelect} type="button">
                <MessageSquareIcon aria-hidden className="size-3.5 shrink-0 text-muted-foreground" />

                <span className={twMerge('truncate', isCurrent && 'font-semibold')}>{getChatDisplayTitle(chat)}</span>

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
                                aria-label="Chat is waiting for your answer or approval"
                                className="size-3.5 shrink-0 text-content-warning-primary"
                                role="img"
                            />
                        </TooltipTrigger>

                        <TooltipContent side="right">This chat is waiting for your answer or approval.</TooltipContent>
                    </Tooltip>
                )}
            </button>

            <div className="ml-1 flex shrink-0 items-center gap-0.5">
                {titleGenerationError && (
                    <button
                        aria-label="Retry title generation"
                        className="rounded p-0.5 text-content-warning-primary hover:bg-muted"
                        disabled={isRetryingTitle}
                        onClick={handleRetryTitleGeneration}
                        title={`Title generation failed: ${titleGenerationError}. Click to retry.`}
                        type="button"
                    >
                        <RotateCcwIcon className={twMerge('size-3.5', isRetryingTitle && 'animate-spin')} />
                    </button>
                )}

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <button
                            aria-label="Chat actions"
                            className="hidden rounded p-0.5 group-focus-within:flex group-hover:flex hover:bg-muted focus:flex data-[state=open]:flex"
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

                        <DropdownMenuSeparator />

                        <DropdownMenuItem onClick={onDelete} variant="destructive">
                            <Trash2Icon />
                            Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>
        </div>
    );
};

/**
 * The AI Hub's left sidebar: New Chat, the Active / Archived toggle, search, and the paged chat list. Each row carries
 * the running / paused indicator the runtime keeps in the chats store, a retry control for a failed title, and the
 * rename / archive / delete menu.
 */
const AiHubChatsSidebar = () => {
    const [visibleCount, setVisibleCount] = useState(CHATS_PAGE_SIZE);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const activeFilter = useAiHubChatsStore((state) => state.activeFilter);
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);
    const searchTerm = useAiHubChatsStore((state) => state.searchTerm);
    const setActiveFilter = useAiHubChatsStore((state) => state.setActiveFilter);
    const setSearchTerm = useAiHubChatsStore((state) => state.setSearchTerm);

    const {pathname} = useLocation();

    const switchChat = useSwitchChat();
    const chatActions = useAiHubChatActions();

    const isViewingArchived = activeFilter === 'ARCHIVED';

    const {data: chats, isLoading} = useAiHubChatsQuery(
        currentWorkspaceId,
        currentEnvironmentId,
        isViewingArchived ? 'ARCHIVED' : 'ACTIVE'
    );

    const filteredChats = useMemo(() => {
        if (!chats) {
            return [];
        }

        const lowerSearch = searchTerm.toLowerCase();

        if (!lowerSearch) {
            return chats;
        }

        return chats.filter((chat) => matchesSearch(chat, lowerSearch));
    }, [chats, searchTerm]);

    const {hiddenCount, visibleChats} = useMemo(
        () => getChatsPage(filteredChats, visibleCount),
        [filteredChats, visibleCount]
    );

    const isOnNewChat = pathname === '/automation/ai-hub';

    useEffect(() => {
        setVisibleCount(CHATS_PAGE_SIZE);
    }, [searchTerm, activeFilter]);

    return (
        <div className="flex flex-col gap-2 pb-2">
            <div className="flex flex-col gap-0.5">
                <Link
                    className={twMerge(
                        'flex items-center gap-2 rounded-md px-2 py-1.5 text-sm font-medium text-muted-foreground hover:bg-accent hover:text-foreground',
                        isOnNewChat && 'bg-accent font-semibold text-foreground'
                    )}
                    to="/automation/ai-hub"
                >
                    <MessageSquarePlusIcon className="size-4 shrink-0 text-muted-foreground" />

                    <span>New Chat</span>
                </Link>
            </div>

            <div className="flex items-center justify-between gap-2 px-2">
                <h4 className="text-sm font-semibold text-foreground">
                    {isViewingArchived ? 'Archived chats' : 'Chats'}
                </h4>

                <Tooltip>
                    <TooltipTrigger asChild>
                        <button
                            aria-label={isViewingArchived ? 'Back to active chats' : 'Archive'}
                            aria-pressed={isViewingArchived}
                            className={twMerge(
                                '-mr-1.5 inline-flex size-7 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground',
                                isViewingArchived && 'bg-accent text-foreground'
                            )}
                            onClick={() => setActiveFilter(isViewingArchived ? 'ACTIVE' : 'ARCHIVED')}
                            type="button"
                        >
                            {isViewingArchived ? (
                                <ChevronLeftIcon className="size-4" />
                            ) : (
                                <ArchiveIcon className="size-4" />
                            )}
                        </button>
                    </TooltipTrigger>

                    <TooltipContent>{isViewingArchived ? 'Back to active' : 'Archive'}</TooltipContent>
                </Tooltip>
            </div>

            <Input
                onChange={(event) => setSearchTerm(event.target.value)}
                placeholder="Search chats..."
                value={searchTerm}
            />

            {isLoading ? (
                <ChatsSidebarSkeleton />
            ) : filteredChats.length === 0 ? (
                <div className="px-2 py-4">
                    <span className="text-xs text-muted-foreground">
                        {isViewingArchived ? 'No archived chats yet.' : 'No chats yet.'}
                    </span>
                </div>
            ) : (
                <div className="flex flex-col gap-0.5">
                    {visibleChats.map((chat) => (
                        <ChatItem
                            chat={chat}
                            isCurrent={currentChatId === chat.id}
                            key={chat.id}
                            onArchive={() => chatActions.archiveChat(chat)}
                            onDelete={() => chatActions.requestDelete(chat)}
                            onRename={() => chatActions.requestRename(chat)}
                            onSelect={() => void switchChat(chat)}
                            onUnarchive={() => chatActions.unarchiveChat(chat)}
                            workspaceId={currentWorkspaceId}
                        />
                    ))}

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

            <AiHubChatActionDialogs {...chatActions} />
        </div>
    );
};

export default AiHubChatsSidebar;
