import Button from '@/components/Button/Button';
import {ListFilterIcon} from 'lucide-react';
import {useShallow} from 'zustand/react/shallow';

import {useApprovalTasksStore} from '../stores/useApprovalTasksStore';
import ApprovalTaskActiveFilterBadges from './ApprovalTaskActiveFilterBadges';
import ApprovalTaskCard from './ApprovalTaskCard';
import ApprovalTaskFilters from './ApprovalTaskFilters';
import ApprovalTaskSearch from './ApprovalTaskSearch';
import ApprovalTaskSortMenu from './ApprovalTaskSortMenu';
import {useApprovalTaskList} from './hooks/useApprovalTaskList';

export default function ApprovalTaskList() {
    const {hasActiveFilters, searchQuery, selectedApprovalTaskId} = useApprovalTasksStore(
        useShallow((state) => ({
            hasActiveFilters: state.hasActiveFilters(),
            searchQuery: state.searchQuery,
            selectedApprovalTaskId: state.selectedApprovalTaskId,
        }))
    );

    const {
        emptyStateMessage,
        filteredApprovalTasks,
        handleClearFilters,
        handleSelectApprovalTask,
        handleSortChange,
        handleStatusToggle,
        headerText,
        sortBy,
        sortDirection,
        totalApprovalTaskCount,
    } = useApprovalTaskList();

    return (
        <div className="flex w-96 shrink-0 flex-col border-r border-border bg-background">
            <div className="shrink-0 border-b border-border p-4">
                <div className="mb-3 flex items-center justify-between">
                    <h2 className="text-lg font-semibold text-foreground">{headerText}</h2>

                    <div className="flex items-center gap-1">
                        <ApprovalTaskSortMenu
                            onSortChange={handleSortChange}
                            sortBy={sortBy}
                            sortDirection={sortDirection}
                        />

                        <ApprovalTaskFilters />
                    </div>
                </div>

                <ApprovalTaskActiveFilterBadges />

                <ApprovalTaskSearch />
            </div>

            <div className="min-w-0 flex-1 overflow-y-auto">
                <div className="min-w-0 space-y-2 p-4">
                    {filteredApprovalTasks.length > 0 ? (
                        filteredApprovalTasks.map((approvalTask) => (
                            <ApprovalTaskCard
                                approvalTask={approvalTask}
                                isSelected={selectedApprovalTaskId === approvalTask.id}
                                key={approvalTask.id}
                                onSelect={() => approvalTask.id && handleSelectApprovalTask(approvalTask.id)}
                                onStatusToggle={(event) =>
                                    approvalTask.id && handleStatusToggle(approvalTask.id, event)
                                }
                            />
                        ))
                    ) : (
                        <div className="flex flex-col items-center justify-center py-12 text-center">
                            <div className="mb-4 rounded-full bg-muted p-3">
                                <ListFilterIcon className="size-6 text-muted-foreground" />
                            </div>

                            <h3 className="mb-1 font-medium text-foreground">No approval tasks found</h3>

                            <p className="mb-4 text-sm text-muted-foreground">{emptyStateMessage}</p>

                            {(searchQuery || hasActiveFilters) && (
                                <Button
                                    label="Clear filters"
                                    onClick={handleClearFilters}
                                    size="sm"
                                    variant="outline"
                                />
                            )}
                        </div>
                    )}
                </div>
            </div>

            <div className="flex shrink-0 items-center justify-between border-t border-border bg-muted/30 px-4 py-2 text-xs text-muted-foreground">
                <span>
                    {filteredApprovalTasks.length} of {totalApprovalTaskCount} approval tasks
                </span>
            </div>
        </div>
    );
}
