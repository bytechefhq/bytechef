import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import EmptyList from '@/components/EmptyList';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import {Input} from '@/components/ui/input';
import MemoryDeleteDialog from '@/pages/automation/ai/memories/dialogs/MemoryDeleteDialog';
import MemoryDetailDialog from '@/pages/automation/ai/memories/dialogs/MemoryDetailDialog';
import MemoryEditDialog from '@/pages/automation/ai/memories/dialogs/MemoryEditDialog';
import {MemoryDialogDataContext} from '@/pages/automation/ai/memories/dialogs/memoryDialogData';
import {
    AI_AUTO_MEMORY_TYPES,
    AI_AUTO_MEMORY_TYPE_META,
    AiAutoMemoryTypeType,
    getAiAutoMemoryTypeLabel,
} from '@/pages/automation/ai/memories/hooks/useAiAutoMemories';
import {MEMORIES_DOCUMENTATION_URL} from '@/pages/settings/settingsDocumentationUrls';
import FilterBadges from '@/shared/components/filters/FilterBadges';
import FilterMenu, {type FilterGroupI, hasActiveFilters} from '@/shared/components/filters/FilterMenu';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {formatDistanceToNow} from 'date-fns';
import {BrainIcon, EllipsisVerticalIcon, PencilIcon, SearchIcon, Trash2Icon, TriangleAlertIcon} from 'lucide-react';
import {useMemo, useState} from 'react';

import {
    EMBEDDED_MEMORY_DIALOG_DATA,
    EMBEDDED_MEMORY_WORKSPACE_ID,
    EmbeddedAiAutoMemoryI,
    getConnectedUserLabel,
    useEmbeddedAiAutoMemoriesQuery,
    useEmbeddedAiAutoMemoryConnectedUsersQuery,
} from './hooks/useEmbeddedAiAutoMemories';

const ALL = 'ALL';

const TYPE_FILTER_OPTIONS = [
    {label: 'All', value: ALL},
    ...AI_AUTO_MEMORY_TYPES.map((memoryType) => ({
        label: AI_AUTO_MEMORY_TYPE_META[memoryType].label,
        value: memoryType,
    })),
];

const COLUMN_HEADER_CLASS_NAME = 'px-4 py-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase';

function findMemory(memories: EmbeddedAiAutoMemoryI[] | undefined, memoryId: number | null) {
    if (memoryId === null) {
        return null;
    }

    return memories?.find((memory) => memory.id === memoryId) || null;
}

function formatRelative(value: string): string {
    try {
        return formatDistanceToNow(new Date(value), {addSuffix: true});
    } catch (formatError) {
        console.warn('EmbeddedMemories: formatRelative failed', {
            message: formatError instanceof Error ? formatError.message : String(formatError),
            value,
        });

        return '';
    }
}

interface EmbeddedMemoriesTableRowPropsI {
    memory: EmbeddedAiAutoMemoryI;
    onDelete: (memory: EmbeddedAiAutoMemoryI) => void;
    onEdit: (memory: EmbeddedAiAutoMemoryI) => void;
    onView: (memory: EmbeddedAiAutoMemoryI) => void;
}

const EmbeddedMemoriesTableRow = ({memory, onDelete, onEdit, onView}: EmbeddedMemoriesTableRowPropsI) => {
    const relativeTime = useMemo(() => formatRelative(memory.updatedAt), [memory.updatedAt]);

    return (
        <tr
            className="cursor-pointer border-b last:border-0 hover:bg-muted/40 focus-visible:bg-muted/40 focus-visible:outline-none"
            onClick={() => onView(memory)}
            onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();

                    onView(memory);
                }
            }}
            tabIndex={0}
        >
            <td className="px-4 py-2">
                <div className="flex flex-col">
                    <span className="text-sm font-medium">{memory.title}</span>

                    <span className="text-xs text-muted-foreground">{memory.name}</span>
                </div>
            </td>

            <td className="px-4 py-2">
                <Badge label={getAiAutoMemoryTypeLabel(memory.memoryType)} styleType="secondary-outline" />
            </td>

            <td className="px-4 py-2">
                {memory.connectedUser ? (
                    <div className="flex flex-col">
                        <span className="text-sm">{getConnectedUserLabel(memory.connectedUser)}</span>

                        <span className="text-xs text-muted-foreground">{memory.connectedUser.externalId}</span>
                    </div>
                ) : (
                    <span className="text-sm text-muted-foreground">Deleted connected user</span>
                )}
            </td>

            <td className="px-4 py-2 text-sm">
                {memory.integration ? (
                    memory.integration.name || memory.integration.componentName
                ) : (
                    <span className="text-muted-foreground">Deleted integration</span>
                )}
            </td>

            <td className="px-4 py-2 text-sm text-muted-foreground">{relativeTime}</td>

            <td className="w-px px-4 py-2 text-right">
                <DropdownMenu>
                    <DropdownMenuTrigger asChild onClick={(event) => event.stopPropagation()}>
                        <Button
                            aria-label={`More actions for ${memory.title}`}
                            icon={<EllipsisVerticalIcon />}
                            size="icon"
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end" onClick={(event) => event.stopPropagation()}>
                        <DropdownMenuItem icon={<PencilIcon />} label="Edit" onClick={() => onEdit(memory)} />

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            icon={<Trash2Icon />}
                            label="Delete"
                            onClick={() => onDelete(memory)}
                            variant="destructive"
                        />
                    </DropdownMenuContent>
                </DropdownMenu>
            </td>
        </tr>
    );
};

const EmbeddedMemories = () => {
    const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);
    const [editTargetId, setEditTargetId] = useState<number | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedConnectedUserId, setSelectedConnectedUserId] = useState<number | null>(null);
    const [selectedMemoryType, setSelectedMemoryType] = useState<AiAutoMemoryTypeType | null>(null);
    const [viewTargetId, setViewTargetId] = useState<number | null>(null);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {
        data: connectedUsers,
        isError: isConnectedUsersError,
        refetch: refetchConnectedUsers,
    } = useEmbeddedAiAutoMemoryConnectedUsersQuery(currentEnvironmentId);

    const selectedConnectedUser = connectedUsers?.find((connectedUser) => connectedUser.id === selectedConnectedUserId);

    const {
        data: memories,
        error: memoriesError,
        isError: isMemoriesError,
        isLoading,
        refetch: refetchMemories,
    } = useEmbeddedAiAutoMemoriesQuery(
        currentEnvironmentId,
        selectedConnectedUser?.id,
        selectedMemoryType || undefined
    );

    const filterGroups = useMemo<FilterGroupI[]>(() => {
        const groups: FilterGroupI[] = [];

        if (connectedUsers && connectedUsers.length > 0) {
            groups.push({
                allValue: ALL,
                key: 'connectedUser',
                label: 'Connected User',
                onChange: (value) => setSelectedConnectedUserId(value === ALL ? null : Number(value)),
                options: [
                    {label: 'All', value: ALL},
                    ...connectedUsers.map((connectedUser) => ({
                        label: getConnectedUserLabel(connectedUser),
                        value: String(connectedUser.id),
                    })),
                ],
                value: selectedConnectedUser ? String(selectedConnectedUser.id) : ALL,
            });
        }

        groups.push({
            allValue: ALL,
            key: 'type',
            label: 'Type',
            onChange: (value) => setSelectedMemoryType(value === ALL ? null : (value as AiAutoMemoryTypeType)),
            options: TYPE_FILTER_OPTIONS,
            value: selectedMemoryType || ALL,
        });

        return groups;
    }, [connectedUsers, selectedConnectedUser, selectedMemoryType]);

    const filteredMemories = useMemo(() => {
        if (!memories) {
            return [];
        }

        const lowerSearch = searchTerm.trim().toLowerCase();

        if (!lowerSearch) {
            return memories;
        }

        return memories.filter((memory) => {
            const title = memory.title.toLowerCase();
            const description = (memory.description ?? '').toLowerCase();

            return title.includes(lowerSearch) || description.includes(lowerSearch);
        });
    }, [memories, searchTerm]);

    const deleteTarget = useMemo(() => findMemory(memories, deleteTargetId), [deleteTargetId, memories]);
    const editTarget = useMemo(() => findMemory(memories, editTargetId), [editTargetId, memories]);
    const viewTarget = useMemo(() => findMemory(memories, viewTargetId), [memories, viewTargetId]);

    const filtersActive = hasActiveFilters(filterGroups);

    const totalCount = memories?.length ?? 0;

    const showLoadError = isMemoriesError && memories === undefined;

    return (
        <MemoryDialogDataContext.Provider value={EMBEDDED_MEMORY_DIALOG_DATA}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        description="Facts integration agents stored for each connected user."
                        position="main"
                        right={
                            <div className="flex items-center gap-2">
                                <div className="relative w-64">
                                    <SearchIcon className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />

                                    <Input
                                        className="pl-9"
                                        onChange={(event) => setSearchTerm(event.target.value)}
                                        placeholder="Search by title or description..."
                                        value={searchTerm}
                                    />
                                </div>

                                <FilterMenu groups={filterGroups} title="Filter Memories" />
                            </div>
                        }
                        title="AI Memories"
                    />
                }
                leftSidebarOpen={false}
            >
                <PageLoader className="min-h-full" loading={isLoading}>
                    <div className="flex w-full flex-col p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
                        {filtersActive && (
                            <div className="flex flex-wrap items-center gap-2 pb-4">
                                <FilterBadges groups={filterGroups} />
                            </div>
                        )}

                        {isConnectedUsersError && (
                            <div
                                className="mb-4 flex items-center gap-3 rounded-md border px-4 py-2 text-sm text-muted-foreground"
                                role="alert"
                            >
                                <TriangleAlertIcon className="size-4 shrink-0" />

                                <span className="flex-1">
                                    Could not load connected users, so the Connected User filter is unavailable.
                                </span>

                                <Button
                                    label="Retry"
                                    onClick={() => refetchConnectedUsers()}
                                    size="sm"
                                    variant="outline"
                                />
                            </div>
                        )}

                        {showLoadError && (
                            <div className="flex flex-1 items-center justify-center">
                                <EmptyList
                                    button={
                                        <Button label="Retry" onClick={() => refetchMemories()} variant="outline" />
                                    }
                                    className="mx-auto max-w-2xl px-6"
                                    icon={<TriangleAlertIcon className="size-24 text-stroke-neutral-tertiary" />}
                                    message={memoriesError?.message || 'The memories could not be loaded.'}
                                    title="Could not load memories"
                                />
                            </div>
                        )}

                        {!showLoadError && totalCount === 0 && filtersActive && (
                            <p className="p-8 text-center text-sm text-muted-foreground">
                                No memories match the selected filters.
                            </p>
                        )}

                        {!showLoadError && totalCount === 0 && !filtersActive && (
                            <div className="flex flex-1 items-center justify-center">
                                <EmptyState
                                    description="Integration agents that use the Auto Memory tool store what they learn about each connected user here. You can inspect, edit or delete each memory."
                                    docsLink={{href: MEMORIES_DOCUMENTATION_URL, label: 'How memories work'}}
                                    icon={<BrainIcon />}
                                    title="No memories yet"
                                />
                            </div>
                        )}

                        {!showLoadError && totalCount > 0 && (
                            <div className="flex w-full flex-1 flex-col gap-4">
                                {filteredMemories.length === 0 ? (
                                    <p className="p-8 text-center text-sm text-muted-foreground">
                                        No memories match your search.
                                    </p>
                                ) : (
                                    <div className="overflow-x-auto rounded-md border">
                                        <table className="w-full">
                                            <thead>
                                                <tr className="border-b bg-muted/50 text-left">
                                                    <th className={COLUMN_HEADER_CLASS_NAME}>Title</th>

                                                    <th className={COLUMN_HEADER_CLASS_NAME}>Type</th>

                                                    <th className={COLUMN_HEADER_CLASS_NAME}>Connected User</th>

                                                    <th className={COLUMN_HEADER_CLASS_NAME}>Integration</th>

                                                    <th className={COLUMN_HEADER_CLASS_NAME}>Updated</th>

                                                    <th className={`w-px text-right ${COLUMN_HEADER_CLASS_NAME}`}>
                                                        Actions
                                                    </th>
                                                </tr>
                                            </thead>

                                            <tbody>
                                                {filteredMemories.map((memory) => (
                                                    <EmbeddedMemoriesTableRow
                                                        key={memory.id}
                                                        memory={memory}
                                                        onDelete={(target) => setDeleteTargetId(target.id)}
                                                        onEdit={(target) => setEditTargetId(target.id)}
                                                        onView={(target) => setViewTargetId(target.id)}
                                                    />
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </PageLoader>

                <MemoryDetailDialog
                    memory={viewTarget}
                    onClose={() => setViewTargetId(null)}
                    open={viewTarget !== null}
                    workspaceId={EMBEDDED_MEMORY_WORKSPACE_ID}
                />

                <MemoryEditDialog
                    environmentId={currentEnvironmentId}
                    memory={editTarget}
                    onClose={() => setEditTargetId(null)}
                    open={editTargetId !== null}
                    workspaceId={EMBEDDED_MEMORY_WORKSPACE_ID}
                />

                <MemoryDeleteDialog
                    environmentId={currentEnvironmentId}
                    memory={deleteTarget}
                    onClose={() => setDeleteTargetId(null)}
                    open={deleteTarget !== null}
                    workspaceId={EMBEDDED_MEMORY_WORKSPACE_ID}
                />
            </LayoutContainer>
        </MemoryDialogDataContext.Provider>
    );
};

export default EmbeddedMemories;
