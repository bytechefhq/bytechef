import {MouseEvent, useCallback, useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useTasksStore} from '../../stores/useTasksStore';
import {getHeaderText, sortTasks} from '../../utils/task-utils';

import type {SortDirectionType, SortOptionType, TaskI} from '../../types/types';

export interface UseTaskListReturnI {
    // Task data
    emptyStateMessage: string;
    filteredTasks: TaskI[];
    headerText: string;
    totalTaskCount: number;

    // Sort
    handleSortChange: (sortBy: SortOptionType, direction: SortDirectionType) => void;
    sortBy: SortOptionType;
    sortDirection: SortDirectionType;

    // Task actions
    handleClearFilters: () => void;
    handleSelectTask: (taskId: string) => void;
    handleStatusToggle: (taskId: string, event: MouseEvent) => void;
}

export function useTaskList(): UseTaskListReturnI {
    const [sortBy, setSortBy] = useState<SortOptionType>('created');
    const [sortDirection, setSortDirection] = useState<SortDirectionType>('desc');

    const {
        cycleTaskStatus,
        filters,
        hasActiveFilters,
        resetFilters,
        searchQuery,
        setSearchQuery,
        setSelectedTaskId,
        tasks,
    } = useTasksStore(
        useShallow((state) => ({
            cycleTaskStatus: state.cycleTaskStatus,
            filters: state.filters,
            hasActiveFilters: state.hasActiveFilters(),
            resetFilters: state.resetFilters,
            searchQuery: state.searchQuery,
            setSearchQuery: state.setSearchQuery,
            setSelectedTaskId: state.setSelectedTaskId,
            tasks: state.tasks,
        }))
    );

    // Computed values
    const filteredTasks = useMemo(() => {
        const filtered = tasks.filter((task) => {
            const matchesSearch =
                task.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                task.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
                task.assignee.toLowerCase().includes(searchQuery.toLowerCase());

            const matchesStatus = filters.status === 'all' || task.status === filters.status;
            const matchesPriority = filters.priority === 'all' || task.priority === filters.priority;
            const matchesAssignee = filters.assignee === 'all' || task.assignee === filters.assignee;

            return matchesSearch && matchesStatus && matchesPriority && matchesAssignee;
        });

        return sortTasks(filtered, sortBy, sortDirection);
    }, [tasks, searchQuery, filters, sortBy, sortDirection]);

    const emptyStateMessage = useMemo(() => {
        if (searchQuery) {
            return `No tasks match "${searchQuery}"`;
        }

        if (hasActiveFilters) {
            return 'No tasks match the current filters';
        }

        return 'You will see here tasks assigned to you';
    }, [searchQuery, hasActiveFilters]);

    const headerText = getHeaderText(filters);

    // Clear filters handler
    const handleClearFilters = useCallback(() => {
        setSearchQuery('');
        resetFilters();
    }, [setSearchQuery, resetFilters]);

    // Selection handler
    const handleSelectTask = useCallback(
        (taskId: string) => {
            setSelectedTaskId(taskId);
        },
        [setSelectedTaskId]
    );

    // Sort handler
    const handleSortChange = useCallback((newSortBy: SortOptionType, direction: SortDirectionType) => {
        setSortBy(newSortBy);
        setSortDirection(direction);
    }, []);

    // Status toggle handler
    const handleStatusToggle = useCallback(
        (taskId: string, event: MouseEvent) => {
            event.stopPropagation();

            cycleTaskStatus(taskId);
        },
        [cycleTaskStatus]
    );

    return {
        emptyStateMessage,
        filteredTasks,
        handleClearFilters,
        handleSelectTask,
        handleSortChange,
        handleStatusToggle,
        headerText,
        sortBy,
        sortDirection,
        totalTaskCount: tasks.length,
    };
}
