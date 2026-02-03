import Button from '@/components/Button/Button';
import {ScrollArea} from '@/components/ui/scroll-area';
import {ListFilterIcon, PlusIcon} from 'lucide-react';
import {useShallow} from 'zustand/react/shallow';

import {useTasksStore} from '../stores/useTasksStore';
import TaskActiveFilterBadges from './TaskActiveFilterBadges';
import TaskCard from './TaskCard';
import TaskCreateDialog from './TaskCreateDialog';
import TaskFilters from './TaskFilters';
import TaskSearch from './TaskSearch';
import TaskSortMenu from './TaskSortMenu';
import {useTaskList} from './hooks/useTaskList';

export default function TaskList() {
    const {hasActiveFilters, searchQuery, selectedTaskId} = useTasksStore(
        useShallow((state) => ({
            hasActiveFilters: state.hasActiveFilters(),
            searchQuery: state.searchQuery,
            selectedTaskId: state.selectedTaskId,
        }))
    );

    const {
        emptyStateMessage,
        filteredTasks,
        handleClearFilters,
        handleSelectTask,
        handleSortChange,
        handleStatusToggle,
        headerText,
        sortBy,
        sortDirection,
        totalTaskCount,
    } = useTaskList();

    return (
        <div className="flex w-96 shrink-0 flex-col border-r border-border bg-background">
            <div className="shrink-0 border-b border-border p-4">
                <div className="mb-3 flex items-center justify-between">
                    <h2 className="text-lg font-semibold text-foreground">{headerText}</h2>

                    <div className="flex items-center gap-1">
                        <TaskSortMenu onSortChange={handleSortChange} sortBy={sortBy} sortDirection={sortDirection} />

                        <TaskFilters />

                        <TaskCreateDialog
                            trigger={
                                <Button
                                    aria-label="Create new task"
                                    className="size-6 shrink-0"
                                    icon={<PlusIcon className="size-4" />}
                                    size="icon"
                                    variant="default"
                                />
                            }
                        />
                    </div>
                </div>

                <TaskActiveFilterBadges />

                <TaskSearch />
            </div>

            <ScrollArea className="flex-1">
                <div className="space-y-2 p-4">
                    {filteredTasks.length > 0 ? (
                        filteredTasks.map((task) => (
                            <TaskCard
                                isSelected={selectedTaskId === task.id}
                                key={task.id}
                                onSelect={() => handleSelectTask(task.id)}
                                onStatusToggle={(event) => handleStatusToggle(task.id, event)}
                                task={task}
                            />
                        ))
                    ) : (
                        <div className="flex flex-col items-center justify-center py-12 text-center">
                            <div className="mb-4 rounded-full bg-muted p-3">
                                <ListFilterIcon className="size-6 text-muted-foreground" />
                            </div>

                            <h3 className="mb-1 font-medium text-foreground">No tasks found</h3>

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
            </ScrollArea>

            <div className="flex shrink-0 items-center justify-between border-t border-border bg-muted/30 px-4 py-2 text-xs text-muted-foreground">
                <span>
                    {filteredTasks.length} of {totalTaskCount} tasks
                </span>
            </div>
        </div>
    );
}
