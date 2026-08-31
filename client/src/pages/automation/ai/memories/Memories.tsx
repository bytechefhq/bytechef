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
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import LeftSidebarToggle from '@/shared/layout/LeftSidebarToggle';
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

    const activeFilterLabel = FILTER_ITEMS.find((item) => item.value === activeFilter)?.label ?? 'All';

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
            // The header surfaces the active Type filter as the page title (echoes the sidebar selection so the
            // user can confirm what's being shown), with the env selector and search input on the right —
            // matching the AssetFiles toolbar order so the search affordance lives in the same screen region
            // across automation pages.
            header={
                <div className="flex w-full items-center gap-2 px-6 py-3">
                    <LeftSidebarToggle />

                    <h1 className="text-base font-semibold">{activeFilterLabel}</h1>

                    {totalCount > 0 && (
                        <span className="text-xs text-muted-foreground">
                            {totalCount} {totalCount === 1 ? 'memory' : 'memories'}
                        </span>
                    )}

                    <div className="ml-auto flex items-center gap-2">
                        <div className="relative w-64">
                            <SearchIcon className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />

                            <Input
                                className="pl-9"
                                onChange={(event) => setSearchTerm(event.target.value)}
                                placeholder="Search by title or description..."
                                value={searchTerm}
                            />
                        </div>
                    </div>
                </div>
            }
            leftSidebarBody={
                <>
                    {principals && principals.length > 0 && (
                        <LeftSidebarNav
                            body={[
                                // Sending no principal is the server's All scope: every owner the caller may
                                // address, which for a non-admin is just their own memories. It leads the group
                                // because it is the default the page opens on.
                                <LeftSidebarNavItem
                                    item={{
                                        current: !selectedPrincipalKey,
                                        id: ALL_OWNERS,
                                        name: 'All',
                                        onItemClick: () => setSelectedPrincipalKey(null),
                                    }}
                                    key={ALL_OWNERS}
                                />,
                                ...principals.map((principal) => (
                                    <LeftSidebarNavItem
                                        item={{
                                            current: selectedPrincipalKey === principalKey(principal),
                                            id: principalKey(principal),
                                            // The label is resolved server-side ("My memories" for the caller, the
                                            // deployment's name otherwise) and rendered verbatim — a client-derived
                                            // label would read "User", which in this sidebar already means a memory
                                            // CATEGORY.
                                            name: principal.label,
                                            onItemClick: (id) => setSelectedPrincipalKey(String(id)),
                                        }}
                                        key={principalKey(principal)}
                                    />
                                )),
                            ]}
                            title="Owner"
                        />
                    )}

                    {/* Owner and Type are independent facets, so one item in EACH is legitimately active at once.
                        Without a divider the two groups read as a single single-select list and the two highlights
                        look like a bug, so the Type group is ruled off from the Owner group above it. */}

                    <LeftSidebarNav
                        body={FILTER_ITEMS.map((item) => (
                            <LeftSidebarNavItem
                                item={{
                                    current: activeFilter === item.value,
                                    id: item.value,
                                    name: item.label,
                                    onItemClick: (id) => setActiveFilter(id as FilterValueType),
                                }}
                                key={item.value}
                            />
                        ))}
                        className={principals && principals.length > 0 ? 'mt-2 border-t border-border/50 pt-4' : ''}
                        title="Type"
                    />
                </>
            }
            leftSidebarHeader={<Header position="sidebar" title="Memories" />}
            leftSidebarWidth="64"
        >
            <PageLoader className="min-h-full" loading={isLoading}>
                {/* EmptyList centers on its parent's CROSS axis, so it only lands mid-page as a direct child
                    of the layout's flex ROW; inside the padded flex COLUMN below it would pin to the top. */}

                {totalCount === 0 ? (
                    <MemoriesEmptyState />
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
                                                // A USER-owned row is always the caller's own (the server only ever
                                                // addresses the caller under USER); anything else is
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
