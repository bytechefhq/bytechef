import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Input} from '@/components/ui/input';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import FilterBadges from '@/shared/components/filters/FilterBadges';
import FilterMenu, {type FilterGroupI, hasActiveFilters} from '@/shared/components/filters/FilterMenu';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {formatDistanceToNow} from 'date-fns';
import {
    BrainIcon,
    EllipsisVerticalIcon,
    EyeIcon,
    PencilIcon,
    SearchIcon,
    Trash2Icon,
    TriangleAlertIcon,
} from 'lucide-react';
import {useMemo, useState} from 'react';

import MemoryDeleteDialog from './dialogs/MemoryDeleteDialog';
import MemoryDetailDialog from './dialogs/MemoryDetailDialog';
import MemoryEditDialog from './dialogs/MemoryEditDialog';
import {
    AI_AUTO_MEMORY_TYPES,
    AI_AUTO_MEMORY_TYPE_META,
    AiAutoMemoryI,
    AiAutoMemoryPrincipalI,
    AiAutoMemoryTypeType,
    getAiAutoMemoryTypeLabel,
    useAiAutoMemoriesQuery,
    useAiAutoMemoryPrincipalsQuery,
} from './hooks/useAiAutoMemories';

type FilterValueType = AiAutoMemoryTypeType | 'ALL';

// Filter option value for the Owner group's All option. Deliberately not shaped like a principal key, so it can never
// collide.
const ALL_OWNERS = 'ALL_OWNERS';

// A principal is identified by the (type, id) PAIR — the same numeric id under a different principal type is a
// different owner — so the filter option value has to carry both.
function principalKey(principal: AiAutoMemoryPrincipalI): string {
    return `${principal.principalType}:${principal.principalId}`;
}

function findMemory(memories: AiAutoMemoryI[] | undefined, memoryId: number | null): AiAutoMemoryI | null {
    if (memoryId === null) {
        return null;
    }

    return memories?.find((memory) => memory.id === memoryId) || null;
}

const FILTER_ITEMS: {label: string; value: FilterValueType}[] = [
    {label: 'All', value: 'ALL'},
    ...AI_AUTO_MEMORY_TYPES.map((memoryType) => ({
        label: AI_AUTO_MEMORY_TYPE_META[memoryType].label,
        value: memoryType,
    })),
];

function formatRelative(value: string): string {
    try {
        return formatDistanceToNow(new Date(value), {addSuffix: true});
    } catch (formatError) {
        // A server-side regression that emits a malformed timestamp causes the table to render blank
        // "time" cells with zero ops signal. Log so the offending value is recoverable from the console.
        console.warn('Memories: formatRelative failed', {
            message: formatError instanceof Error ? formatError.message : String(formatError),
            value,
        });

        return '';
    }
}

interface MemoriesTableRowPropsI {
    memory: AiAutoMemoryI;
    mutable: boolean;
    onDelete: (memory: AiAutoMemoryI) => void;
    onEdit: (memory: AiAutoMemoryI) => void;
    onView: (memory: AiAutoMemoryI) => void;
}

const MemoriesTableRow = ({memory, mutable, onDelete, onEdit, onView}: MemoriesTableRowPropsI) => {
    const relativeTime = useMemo(() => formatRelative(memory.updatedAt), [memory.updatedAt]);

    return (
        <tr className="border-b last:border-0 hover:bg-muted/40">
            <td className="px-4 py-2">
                <div className="flex flex-col">
                    <span className="text-sm font-medium">{memory.title}</span>

                    <span className="text-xs text-muted-foreground">{memory.name}</span>
                </div>
            </td>

            <td className="px-4 py-2">
                <Badge label={getAiAutoMemoryTypeLabel(memory.memoryType)} styleType="secondary-outline" />
            </td>

            <td className="max-w-sm px-4 py-2 text-sm text-muted-foreground">
                <span className="line-clamp-2">{memory.description || ''}</span>
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

                    <DropdownMenuContent align="end" className="p-0">
                        <DropdownMenuItem className="dropdown-menu-item" onClick={() => onView(memory)}>
                            <EyeIcon /> View
                        </DropdownMenuItem>

                        {/* Not rendered at all rather than rendered disabled: a memory the caller cannot mutate is
                            one the server answers with NotFound, so offering the affordance at all would be an item
                            whose only possible outcome is an error toast. */}

                        {mutable && (
                            <DropdownMenuItem className="dropdown-menu-item" onClick={() => onEdit(memory)}>
                                <PencilIcon /> Edit
                            </DropdownMenuItem>
                        )}

                        {mutable && <DropdownMenuSeparator className="m-0" />}

                        {mutable && (
                            <DropdownMenuItem
                                className="dropdown-menu-item-destructive"
                                onClick={() => onDelete(memory)}
                                variant="destructive"
                            >
                                <Trash2Icon /> Delete
                            </DropdownMenuItem>
                        )}
                    </DropdownMenuContent>
                </DropdownMenu>
            </td>
        </tr>
    );
};

const MemoriesEmptyState = () => (
    <EmptyList
        className="mx-auto max-w-2xl px-6"
        icon={<BrainIcon className="size-24 text-stroke-neutral-tertiary" />}
        message="AI Agents that use the Auto Memory tool write memories here when their workflows run - user preferences, project decisions, corrections, and external reference pointers. Once an agent stores something worth keeping, it shows up on this page and you can inspect, edit, or delete it."
        title="No memories yet"
    />
);

interface MemoriesErrorStatePropsI {
    message: string;
    onRetry: () => void;
}

const MemoriesErrorState = ({message, onRetry}: MemoriesErrorStatePropsI) => (
    <EmptyList
        button={<Button label="Retry" onClick={onRetry} variant="outline" />}
        className="mx-auto max-w-2xl px-6"
        icon={<TriangleAlertIcon className="size-24 text-stroke-neutral-tertiary" />}
        message={message}
        title="Could not load memories"
    />
);

const Memories = () => {
    const [activeFilter, setActiveFilter] = useState<FilterValueType>('ALL');
    const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);
    const [editTargetId, setEditTargetId] = useState<number | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedPrincipalKey, setSelectedPrincipalKey] = useState<string | null>(null);
    const [viewTargetId, setViewTargetId] = useState<number | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    // The server gates mutating a non-USER-owned memory on the tenant-level ROLE_ADMIN authority, not a workspace role.
    const isAdmin = useIsTenantAdmin();

    const {
        data: principals,
        isError: isPrincipalsError,
        refetch: refetchPrincipals,
    } = useAiAutoMemoryPrincipalsQuery(currentWorkspaceId, currentEnvironmentId);

    const memoryType = activeFilter === 'ALL' ? undefined : activeFilter;

    // Resolved against the CURRENT owner list rather than trusted from state: switching environment replaces the
    // owners, and a selection that no longer exists has to fall back to sending no principal (the server's All scope)
    // instead of filtering by an owner that holds nothing here.
    const selectedPrincipal = principals?.find((principal) => principalKey(principal) === selectedPrincipalKey);

    const {
        data: memories,
        error: memoriesError,
        isError: isMemoriesError,
        isLoading,
        refetch: refetchMemories,
    } = useAiAutoMemoriesQuery(
        currentWorkspaceId,
        currentEnvironmentId,
        memoryType,
        selectedPrincipal?.principalType,
        selectedPrincipal?.principalId
    );

    // Owner and Type are independent facets, so one selection in EACH is legitimately active at once.
    const filterGroups = useMemo<FilterGroupI[]>(() => {
        const groups: FilterGroupI[] = [];

        if (principals && principals.length > 0) {
            groups.push({
                allValue: ALL_OWNERS,
                key: 'owner',
                label: 'Owner',
                onChange: (value) => setSelectedPrincipalKey(value === ALL_OWNERS ? null : value),
                // Sending no principal is the server's All scope: every owner the caller may address (see the
                // aiAutoMemories schema description). It leads the list because it is the default the page opens on.
                options: [
                    {label: 'All', value: ALL_OWNERS},
                    // The label is resolved server-side ("My memories" for the caller, the deployment's name
                    // otherwise) and rendered verbatim — a client-derived label would read "User", which in
                    // this menu already means a memory CATEGORY.
                    ...principals.map((principal) => ({
                        label: principal.label,
                        value: principalKey(principal),
                    })),
                ],
                // Reported from the RESOLVED selection, the same one the query uses: a picked owner that no longer
                // resolves (environment switch, owner dropped after a refetch) is the All scope, not a chip with an
                // empty label.
                value: selectedPrincipal ? principalKey(selectedPrincipal) : ALL_OWNERS,
            });
        }

        groups.push({
            allValue: 'ALL',
            key: 'type',
            label: 'Type',
            onChange: (value) => setActiveFilter(value as FilterValueType),
            options: FILTER_ITEMS,
            value: activeFilter,
        });

        return groups;
    }, [activeFilter, principals, selectedPrincipal]);

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

    // Dialogs look their memory up in the latest list rather than holding a snapshot, so an open dialog sees the version
    // a refetch brings — after a rejected save that is what the user reloads to retry. The view and delete dialogs close
    // once the memory is gone; the edit dialog stays open on the version it loaded and says it was deleted.
    const deleteTarget = useMemo(() => findMemory(memories, deleteTargetId), [deleteTargetId, memories]);
    const editTarget = useMemo(() => findMemory(memories, editTargetId), [editTargetId, memories]);
    const viewTarget = useMemo(() => findMemory(memories, viewTargetId), [memories, viewTargetId]);

    const filtersActive = hasActiveFilters(filterGroups);

    const totalCount = memories?.length ?? 0;

    const showLoadError = isMemoriesError && memories === undefined;

    return (
        <LayoutContainer
            header={
                <Header
                    description="Facts the agent has stored while working in this workspace."
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
                <div className="flex w-full flex-1 flex-col">
                    {filtersActive && (
                        <div className="flex flex-wrap items-center gap-2 px-6 pt-4">
                            <FilterBadges groups={filterGroups} />
                        </div>
                    )}

                    {isPrincipalsError && (
                        <div
                            className="mx-6 mt-4 flex items-center gap-3 rounded-md border px-4 py-2 text-sm text-muted-foreground"
                            role="alert"
                        >
                            <TriangleAlertIcon className="size-4 shrink-0" />

                            <span className="flex-1">
                                Could not load memory owners, so the Owner filter is unavailable.
                            </span>

                            <Button label="Retry" onClick={() => refetchPrincipals()} size="sm" variant="outline" />
                        </div>
                    )}

                    {showLoadError && (
                        <div className="flex flex-1 items-center justify-center">
                            <MemoriesErrorState
                                message={memoriesError?.message || 'The memories could not be loaded.'}
                                onRetry={() => refetchMemories()}
                            />
                        </div>
                    )}

                    {/* An empty result under an active filter says nothing about whether memories exist at all, so the
                        onboarding copy is reserved for the unfiltered view. */}

                    {!showLoadError && totalCount === 0 && filtersActive && (
                        <p className="p-8 text-center text-sm text-muted-foreground">
                            No memories match the selected filters.
                        </p>
                    )}

                    {!showLoadError && totalCount === 0 && !filtersActive && (
                        <div className="flex flex-1 items-center justify-center">
                            <MemoriesEmptyState />
                        </div>
                    )}

                    {!showLoadError && totalCount > 0 && (
                        <div className="flex w-full flex-1 flex-col gap-4 p-6">
                            {filteredMemories.length === 0 ? (
                                <p className="p-8 text-center text-sm text-muted-foreground">
                                    No memories match your search.
                                </p>
                            ) : (
                                <div className="overflow-x-auto rounded-md border">
                                    <table className="w-full">
                                        <thead>
                                            <tr className="border-b bg-muted/50 text-left">
                                                <th className="px-4 py-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                                                    Title
                                                </th>

                                                <th className="px-4 py-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                                                    Type
                                                </th>

                                                <th className="px-4 py-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                                                    Description
                                                </th>

                                                <th className="px-4 py-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                                                    Updated
                                                </th>

                                                <th className="w-px px-4 py-2 text-right text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                                                    Actions
                                                </th>
                                            </tr>
                                        </thead>

                                        <tbody>
                                            {filteredMemories.map((memory) => (
                                                <MemoriesTableRow
                                                    key={memory.id}
                                                    memory={memory}
                                                    // A USER-owned row is always the caller's own (the server only
                                                    // ever addresses the caller under USER); anything else is
                                                    // deployment-owned and admin-only to mutate.
                                                    mutable={memory.principalType === 'USER' || isAdmin}
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

            <MemoryDetailDialog memory={viewTarget} onClose={() => setViewTargetId(null)} open={viewTarget !== null} />

            <MemoryEditDialog
                environmentId={currentEnvironmentId}
                memory={editTarget}
                onClose={() => setEditTargetId(null)}
                open={editTargetId !== null}
                workspaceId={currentWorkspaceId}
            />

            <MemoryDeleteDialog
                environmentId={currentEnvironmentId}
                memory={deleteTarget}
                onClose={() => setDeleteTargetId(null)}
                open={deleteTarget !== null}
                workspaceId={currentWorkspaceId}
            />
        </LayoutContainer>
    );
};

export default Memories;
