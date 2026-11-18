import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {type FilterGroupI} from '@/shared/components/filters/FilterMenu';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo, useState} from 'react';

import MemoriesPageLayout from './components/MemoriesPageLayout';
import MemoriesTableRow from './components/MemoriesTableRow';
import {filterMemoriesBySearchTerm, findMemory} from './components/memoriesTableUtils';
import MemoryDeleteDialog from './dialogs/MemoryDeleteDialog';
import MemoryDetailDialog from './dialogs/MemoryDetailDialog';
import MemoryEditDialog from './dialogs/MemoryEditDialog';
import {
    AI_AUTO_MEMORY_TYPES,
    AI_AUTO_MEMORY_TYPE_META,
    AiAutoMemoryPrincipalI,
    AiAutoMemoryTypeType,
    useAiAutoMemoriesQuery,
    useAiAutoMemoryPrincipalsQuery,
} from './hooks/useAiAutoMemories';

type FilterValueType = AiAutoMemoryTypeType | 'ALL';

const ALL_OWNERS = 'ALL_OWNERS';

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

const Memories = () => {
    const [activeFilter, setActiveFilter] = useState<FilterValueType>('ALL');
    const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);
    const [editTargetId, setEditTargetId] = useState<number | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedPrincipalKey, setSelectedPrincipalKey] = useState<string | null>(null);
    const [viewTargetId, setViewTargetId] = useState<number | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const isAdmin = useIsTenantAdmin();

    const {
        data: principals,
        isError: isPrincipalsError,
        refetch: refetchPrincipals,
    } = useAiAutoMemoryPrincipalsQuery(currentWorkspaceId, currentEnvironmentId);

    const memoryType = activeFilter === 'ALL' ? undefined : activeFilter;

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

    const filterGroups = useMemo<FilterGroupI[]>(() => {
        const groups: FilterGroupI[] = [];

        if (principals && principals.length > 0) {
            groups.push({
                allValue: ALL_OWNERS,
                key: 'owner',
                label: 'Project Deployment',
                onChange: (value) => setSelectedPrincipalKey(value === ALL_OWNERS ? null : value),
                options: [
                    {label: 'All', value: ALL_OWNERS},
                    ...principals.map((principal) => ({
                        label: principal.label,
                        value: principalKey(principal),
                    })),
                ],
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

    const filteredMemories = useMemo(() => filterMemoriesBySearchTerm(memories, searchTerm), [memories, searchTerm]);

    const deleteTarget = useMemo(() => findMemory(memories, deleteTargetId), [deleteTargetId, memories]);
    const editTarget = useMemo(() => findMemory(memories, editTargetId), [editTargetId, memories]);
    const viewTarget = useMemo(() => findMemory(memories, viewTargetId), [memories, viewTargetId]);

    const showLoadError = isMemoriesError && memories === undefined;

    return (
        <MemoriesPageLayout
            columnTitles={['Description']}
            description="Facts the agent has stored while working in this workspace."
            emptyStateDescription="AI agents that use the Auto Memory tool store what is worth keeping here, such as user preferences and project decisions. You can inspect, edit or delete each memory."
            filterGroups={filterGroups}
            filterLoadError={
                isPrincipalsError
                    ? {
                          message: 'Could not load memory owners, so the Project Deployment filter is unavailable.',
                          onRetry: () => refetchPrincipals(),
                      }
                    : undefined
            }
            isLoading={isLoading}
            loadError={
                showLoadError
                    ? {
                          message: memoriesError?.message || 'The memories could not be loaded.',
                          onRetry: () => refetchMemories(),
                      }
                    : undefined
            }
            onSearchTermChange={setSearchTerm}
            rows={filteredMemories.map((memory) => (
                <MemoriesTableRow
                    key={memory.id}
                    memory={memory}
                    mutable={memory.principalType === 'USER' || isAdmin}
                    onDelete={(target) => setDeleteTargetId(target.id)}
                    onEdit={(target) => setEditTargetId(target.id)}
                    onView={(target) => setViewTargetId(target.id)}
                >
                    <td className="max-w-sm px-4 py-2 text-sm text-muted-foreground">
                        <span className="line-clamp-2">{memory.description || ''}</span>
                    </td>
                </MemoriesTableRow>
            ))}
            searchTerm={searchTerm}
            totalCount={memories?.length ?? 0}
        >
            <MemoryDetailDialog
                memory={viewTarget}
                onClose={() => setViewTargetId(null)}
                open={viewTarget !== null}
                workspaceId={currentWorkspaceId}
            />

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
        </MemoriesPageLayout>
    );
};

export default Memories;
