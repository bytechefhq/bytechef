import MemoriesPageLayout from '@/pages/automation/ai/memories/components/MemoriesPageLayout';
import MemoriesTableRow from '@/pages/automation/ai/memories/components/MemoriesTableRow';
import {filterMemoriesBySearchTerm, findMemory} from '@/pages/automation/ai/memories/components/memoriesTableUtils';
import MemoryDeleteDialog from '@/pages/automation/ai/memories/dialogs/MemoryDeleteDialog';
import MemoryDetailDialog from '@/pages/automation/ai/memories/dialogs/MemoryDetailDialog';
import MemoryEditDialog from '@/pages/automation/ai/memories/dialogs/MemoryEditDialog';
import {MemoryDialogDataContext} from '@/pages/automation/ai/memories/dialogs/memoryDialogData';
import {
    AI_AUTO_MEMORY_TYPES,
    AI_AUTO_MEMORY_TYPE_META,
    AiAutoMemoryTypeType,
} from '@/pages/automation/ai/memories/hooks/useAiAutoMemories';
import {type FilterGroupI} from '@/shared/components/filters/FilterMenu';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo, useState} from 'react';

import {
    EMBEDDED_MEMORY_DIALOG_DATA,
    EMBEDDED_MEMORY_WORKSPACE_ID,
    EmbeddedAiAutoMemoryConnectedUserI,
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

interface EmbeddedMemoryCellsPropsI {
    memory: EmbeddedAiAutoMemoryI;
}

const EmbeddedMemoryCells = ({memory}: EmbeddedMemoryCellsPropsI) => (
    <>
        <td className="px-4 py-2">
            {memory.connectedUser ? (
                <div className="flex flex-col">
                    <span className="text-sm">{getConnectedUserLabel(memory.connectedUser)}</span>

                    {getConnectedUserLabel(memory.connectedUser) !== memory.connectedUser.externalId && (
                        <span className="text-xs text-muted-foreground">{memory.connectedUser.externalId}</span>
                    )}
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
    </>
);

const EmbeddedMemories = () => {
    const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);
    const [editTargetId, setEditTargetId] = useState<number | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedConnectedUser, setSelectedConnectedUser] = useState<EmbeddedAiAutoMemoryConnectedUserI | null>(null);
    const [selectedMemoryType, setSelectedMemoryType] = useState<AiAutoMemoryTypeType | null>(null);
    const [viewTargetId, setViewTargetId] = useState<number | null>(null);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {
        data: connectedUsers,
        isError: isConnectedUsersError,
        refetch: refetchConnectedUsers,
    } = useEmbeddedAiAutoMemoryConnectedUsersQuery(currentEnvironmentId);

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

    const connectedUserOptions = useMemo(() => {
        const listedConnectedUsers = connectedUsers || [];

        if (
            selectedConnectedUser &&
            !listedConnectedUsers.some((connectedUser) => connectedUser.id === selectedConnectedUser.id)
        ) {
            return [...listedConnectedUsers, selectedConnectedUser];
        }

        return listedConnectedUsers;
    }, [connectedUsers, selectedConnectedUser]);

    const filterGroups = useMemo<FilterGroupI[]>(() => {
        const groups: FilterGroupI[] = [];

        if (connectedUserOptions.length > 0) {
            groups.push({
                allValue: ALL,
                key: 'connectedUser',
                label: 'Connected User',
                onChange: (value) =>
                    setSelectedConnectedUser(
                        connectedUserOptions.find((connectedUser) => String(connectedUser.id) === value) || null
                    ),
                options: [
                    {label: 'All', value: ALL},
                    ...connectedUserOptions.map((connectedUser) => ({
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
    }, [connectedUserOptions, selectedConnectedUser, selectedMemoryType]);

    const filteredMemories = useMemo(() => filterMemoriesBySearchTerm(memories, searchTerm), [memories, searchTerm]);

    const deleteTarget = useMemo(() => findMemory(memories, deleteTargetId), [deleteTargetId, memories]);
    const editTarget = useMemo(() => findMemory(memories, editTargetId), [editTargetId, memories]);
    const viewTarget = useMemo(() => findMemory(memories, viewTargetId), [memories, viewTargetId]);

    const showLoadError = isMemoriesError && memories === undefined;

    return (
        <MemoryDialogDataContext.Provider value={EMBEDDED_MEMORY_DIALOG_DATA}>
            <MemoriesPageLayout
                columnTitles={['Connected User', 'Integration']}
                description="Facts integration agents stored for each connected user."
                emptyStateDescription="Integration agents that use the Auto Memory tool store what they learn about each connected user here. You can inspect, edit or delete each memory."
                filterGroups={filterGroups}
                filterLoadError={
                    isConnectedUsersError
                        ? {
                              message: 'Could not load connected users, so the Connected User filter is unavailable.',
                              onRetry: () => refetchConnectedUsers(),
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
                        mutable
                        onDelete={(target) => setDeleteTargetId(target.id)}
                        onEdit={(target) => setEditTargetId(target.id)}
                        onView={(target) => setViewTargetId(target.id)}
                    >
                        <EmbeddedMemoryCells memory={memory} />
                    </MemoriesTableRow>
                ))}
                searchTerm={searchTerm}
                totalCount={memories?.length ?? 0}
            >
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
            </MemoriesPageLayout>
        </MemoryDialogDataContext.Provider>
    );
};

export default EmbeddedMemories;
