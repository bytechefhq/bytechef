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
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import FilterBadges from '@/shared/components/filters/FilterBadges';
import FilterMenu, {type FilterGroupI} from '@/shared/components/filters/FilterMenu';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {formatDistanceToNow} from 'date-fns';
import {BrainIcon, EllipsisVerticalIcon, EyeIcon, PencilIcon, SearchIcon, Trash2Icon} from 'lucide-react';
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
    useAiAutoMemoriesQuery,
    useAiAutoMemoryPrincipalsQuery,
} from './hooks/useAiAutoMemories';

type FilterValueType = AiAutoMemoryTypeType | 'ALL';

// Nav id for the Owner group's All row. Deliberately not shaped like a principal key, so it can never collide.
const ALL_OWNERS = 'ALL_OWNERS';

// A principal is identified by the (type, id) PAIR — the same numeric id under a different principal type is a
// different owner — so the nav item id has to carry both.
function principalKey(principal: AiAutoMemoryPrincipalI): string {
    return `${principal.principalType}:${principal.principalId}`;
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
                <Badge label={memory.memoryType} styleType="secondary-outline" />
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
        // EmptyList's own px-2 is sized for short messages; this one runs edge to edge without a
        // readable measure to hold it in.
        className="mx-auto max-w-2xl px-6"
        icon={<BrainIcon className="size-24 text-stroke-neutral-tertiary" />}
        message="The agent stores facts here as you chat with it - user preferences, project decisions, corrections, and external reference pointers. Once the agent learns something worth keeping, it will show up on this page and you can inspect, edit, or delete it."
        title="No memories yet"
    />
);

const Memories = () => {
    const [activeFilter, setActiveFilter] = useState<FilterValueType>('ALL');
    const [deleteTarget, setDeleteTarget] = useState<AiAutoMemoryI | null>(null);
    const [editTarget, setEditTarget] = useState<AiAutoMemoryI | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedPrincipalKey, setSelectedPrincipalKey] = useState<string | null>(null);
    const [viewTarget, setViewTarget] = useState<AiAutoMemoryI | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    // The server gates mutating a non-USER-owned memory on the tenant-level ROLE_ADMIN authority (not a workspace
    // role), so this mirrors exactly that authority. Gated on `authenticated` as well, per the convention in
    // useHasWorkspaceRole: `account` can still carry a prior session's authorities during a re-login transition.
    const isAdmin = useAuthenticationStore(
        (state) => state.authenticated && (state.account?.authorities?.includes('ROLE_ADMIN') ?? false)
    );

    const {data: principals} = useAiAutoMemoryPrincipalsQuery(currentWorkspaceId, currentEnvironmentId);

    const memoryType = activeFilter === 'ALL' ? undefined : activeFilter;

    // Owner and Type are independent facets, so one selection in EACH is legitimately active at once.
    const filterGroups = useMemo<FilterGroupI[]>(() => {
        const groups: FilterGroupI[] = [];

        if (principals && principals.length > 0) {
            groups.push({
                allValue: ALL_OWNERS,
                key: 'owner',
                label: 'Owner',
                onChange: (value) => setSelectedPrincipalKey(value === ALL_OWNERS ? null : value),
                // Sending no principal is the server's All scope: every owner the caller may address, which
                // for a non-admin is just their own memories. It leads the list because it is the default the
                // page opens on.
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
                value: selectedPrincipalKey ?? ALL_OWNERS,
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
    }, [activeFilter, principals, selectedPrincipalKey]);

    // Resolved against the CURRENT owner list rather than trusted from state: switching environment replaces the
    // owners, and a selection that no longer exists has to fall back to "no principal" (the signed-in user) instead
    // of filtering by an owner that holds nothing here.
    const selectedPrincipal = principals?.find((principal) => principalKey(principal) === selectedPrincipalKey);

    const {data: memories, isLoading} = useAiAutoMemoriesQuery(
        currentWorkspaceId,
        currentEnvironmentId,
        memoryType,
        selectedPrincipal?.principalType,
        selectedPrincipal?.principalId
    );

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

    const totalCount = memories?.length ?? 0;

    return (
        <LayoutContainer
            header={
                <Header
                    description="Facts the agent has stored while working in this workspace."
                    position="main"
                    right={
                        <div className="flex items-center gap-1">
                            <FilterMenu groups={filterGroups} title="Filter Memories" />

                            <EnvironmentSelect />
                        </div>
                    }
                    title="AI Memories"
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader className="min-h-full" loading={isLoading}>
                <div className="flex w-full flex-1 flex-col">
                    {/* Search and the active-filter chips sit above the results and OUTSIDE the empty-state
                        branch: filtering down to nothing is exactly when the chip that emptied the page has to
                        stay reachable. */}

                    <div className="flex flex-wrap items-center gap-2 px-6 pt-4">
                        <div className="relative w-64">
                            <SearchIcon className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />

                            <Input
                                className="pl-9"
                                onChange={(event) => setSearchTerm(event.target.value)}
                                placeholder="Search by title or description..."
                                value={searchTerm}
                            />
                        </div>

                        <FilterBadges groups={filterGroups} />
                    </div>

                    {totalCount === 0 ? (
                        <div className="flex flex-1 items-center justify-center">
                            <MemoriesEmptyState />
                        </div>
                    ) : (
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

                                                {/* w-px collapses the column to its content: the cell holds one icon
                                                    button, so the header text is what would otherwise size it. */}

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
                                                    onDelete={setDeleteTarget}
                                                    onEdit={setEditTarget}
                                                    onView={setViewTarget}
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

            <MemoryDetailDialog memory={viewTarget} onClose={() => setViewTarget(null)} open={viewTarget !== null} />

            <MemoryEditDialog
                environmentId={currentEnvironmentId}
                memory={editTarget}
                onClose={() => setEditTarget(null)}
                open={editTarget !== null}
                workspaceId={currentWorkspaceId}
            />

            <MemoryDeleteDialog
                environmentId={currentEnvironmentId}
                memory={deleteTarget}
                onClose={() => setDeleteTarget(null)}
                open={deleteTarget !== null}
                workspaceId={currentWorkspaceId}
            />
        </LayoutContainer>
    );
};

export default Memories;
