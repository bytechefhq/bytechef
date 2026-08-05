import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Input} from '@/components/ui/input';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import EnvironmentSelect from '@/shared/components/EnvironmentSelect';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {formatDistanceToNow} from 'date-fns';
import {BrainIcon, EyeIcon, PencilIcon, SearchIcon, Trash2Icon} from 'lucide-react';
import {type ReactNode, useMemo, useState} from 'react';

import MemoryDeleteDialog from './dialogs/MemoryDeleteDialog';
import MemoryDetailDialog from './dialogs/MemoryDetailDialog';
import MemoryEditDialog from './dialogs/MemoryEditDialog';
import {AiAutoMemoryI, AiAutoMemoryTypeType, useAiAutoMemoriesQuery} from './hooks/useAiAutoMemories';

type FilterValueType = AiAutoMemoryTypeType | 'ALL';

const FILTER_ITEMS: {label: string; value: FilterValueType}[] = [
    {label: 'All', value: 'ALL'},
    {label: 'User', value: 'USER'},
    {label: 'Feedback', value: 'FEEDBACK'},
    {label: 'Project', value: 'PROJECT'},
    {label: 'Reference', value: 'REFERENCE'},
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
    onDelete: (memory: AiAutoMemoryI) => void;
    onEdit: (memory: AiAutoMemoryI) => void;
    onView: (memory: AiAutoMemoryI) => void;
}

const MemoriesTableRow = ({memory, onDelete, onEdit, onView}: MemoriesTableRowPropsI) => {
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

            <td className="px-4 py-2">
                <div className="flex items-center gap-1">
                    <Button
                        aria-label={`View ${memory.title}`}
                        icon={<EyeIcon />}
                        onClick={() => onView(memory)}
                        size="iconSm"
                        title="View memory"
                        variant="ghost"
                    />

                    <Button
                        aria-label={`Edit ${memory.title}`}
                        icon={<PencilIcon />}
                        onClick={() => onEdit(memory)}
                        size="iconSm"
                        title="Edit memory"
                        variant="ghost"
                    />

                    <Button
                        aria-label={`Delete ${memory.title}`}
                        icon={<Trash2Icon />}
                        onClick={() => onDelete(memory)}
                        size="iconSm"
                        title="Delete memory"
                        variant="destructiveGhost"
                    />
                </div>
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

interface MemoriesProps {
    // The left-sidebar nav. The standalone /automation/ai/memories page renders its own sidebar with the
    // Type filter (matching the other automation pages); the AI Hub > Context > Memories page passes the
    // AI Hub tasks sidebar so Memories renders inside the AI Hub shell (Context nav visible) — the Type
    // filter then stays in the header, since the sidebar slot is taken.
    renderSidebarNav?: () => ReactNode;
    sidebarTitle?: string;
}

const Memories = ({renderSidebarNav, sidebarTitle = 'AI'}: MemoriesProps = {}) => {
    const [activeFilter, setActiveFilter] = useState<FilterValueType>('ALL');
    const [deleteTarget, setDeleteTarget] = useState<AiAutoMemoryI | null>(null);
    const [editTarget, setEditTarget] = useState<AiAutoMemoryI | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [viewTarget, setViewTarget] = useState<AiAutoMemoryI | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const memoryType = activeFilter === 'ALL' ? undefined : activeFilter;

    const activeFilterLabel = FILTER_ITEMS.find((item) => item.value === activeFilter)?.label ?? 'All';

    const {data: memories, isLoading} = useAiAutoMemoriesQuery(currentWorkspaceId, currentEnvironmentId, memoryType);

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
            // The header surfaces the active filter as the page title (echoes the Type select so the
            // user can confirm what's being shown), with the type filter, env selector and search input
            // on the right — matching the AssetFiles toolbar order so the search affordance lives in the
            // same screen region across automation pages.
            header={
                <div className="flex w-full items-center gap-2 px-6 py-3">
                    <h1 className="text-base font-semibold">{activeFilterLabel}</h1>

                    {totalCount > 0 && (
                        <span className="text-xs text-muted-foreground">
                            {totalCount} {totalCount === 1 ? 'memory' : 'memories'}
                        </span>
                    )}

                    <div className="ml-auto flex items-center gap-2">
                        {renderSidebarNav && (
                            <Select
                                onValueChange={(value) => setActiveFilter(value as FilterValueType)}
                                value={activeFilter}
                            >
                                <SelectTrigger aria-label="Filter by type" className="w-36">
                                    <SelectValue />
                                </SelectTrigger>

                                <SelectContent>
                                    {FILTER_ITEMS.map((item) => (
                                        <SelectItem key={item.value} value={item.value}>
                                            {item.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        )}

                        <EnvironmentSelect />

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
                renderSidebarNav ? (
                    renderSidebarNav()
                ) : (
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
                        title="Type"
                    />
                )
            }
            leftSidebarHeader={<Header position="sidebar" title={renderSidebarNav ? sidebarTitle : 'Memories'} />}
            leftSidebarOpen
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

                                            <th className="px-4 py-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                                                Actions
                                            </th>
                                        </tr>
                                    </thead>

                                    <tbody>
                                        {filteredMemories.map((memory) => (
                                            <MemoriesTableRow
                                                key={memory.id}
                                                memory={memory}
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
                memory={editTarget}
                onClose={() => setEditTarget(null)}
                open={editTarget !== null}
                workspaceId={currentWorkspaceId}
            />

            <MemoryDeleteDialog
                memory={deleteTarget}
                onClose={() => setDeleteTarget(null)}
                open={deleteTarget !== null}
                workspaceId={currentWorkspaceId}
            />
        </LayoutContainer>
    );
};

export default Memories;
