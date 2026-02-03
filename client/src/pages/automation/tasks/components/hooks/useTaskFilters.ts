import {useCallback, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useTasksStore} from '../../stores/useTasksStore';

import type {FiltersI, PriorityCountsI, TaskCountsI} from '../../types/types';

export interface UseTaskFiltersReturnI {
    assigneeCounts: Record<string, number>;
    assignees: string[];
    filters: FiltersI;
    handleAssigneeChange: (assignee: FiltersI['assignee']) => void;
    handlePriorityChange: (priority: FiltersI['priority']) => void;
    handleResetFilters: () => void;
    handleStatusChange: (status: FiltersI['status']) => void;
    hasActiveFilters: boolean;
    priorityCounts: PriorityCountsI;
    taskCounts: TaskCountsI;
}

export function useTaskFilters(): UseTaskFiltersReturnI {
    const {filters, resetFilters, setFilters, tasks} = useTasksStore(
        useShallow((state) => ({
            filters: state.filters,
            resetFilters: state.resetFilters,
            setFilters: state.setFilters,
            tasks: state.tasks,
        }))
    );

    const hasActiveFilters = filters.status !== 'all' || filters.priority !== 'all' || filters.assignee !== 'all';

    const taskCounts = useMemo(
        (): TaskCountsI => ({
            all: tasks.length,
            completed: tasks.filter((task) => task.status === 'completed').length,
            'in-progress': tasks.filter((task) => task.status === 'in-progress').length,
            open: tasks.filter((task) => task.status === 'open').length,
        }),
        [tasks]
    );

    const priorityCounts = useMemo(
        (): PriorityCountsI => ({
            all: tasks.length,
            high: tasks.filter((task) => task.priority === 'high').length,
            low: tasks.filter((task) => task.priority === 'low').length,
            medium: tasks.filter((task) => task.priority === 'medium').length,
        }),
        [tasks]
    );

    const {assigneeCounts, assignees} = useMemo(() => {
        const uniqueAssignees = [...new Set(tasks.map((task) => task.assignee))];
        const counts: Record<string, number> = {
            all: tasks.length,
        };

        uniqueAssignees.forEach((assignee) => {
            counts[assignee] = tasks.filter((task) => task.assignee === assignee).length;
        });

        return {assigneeCounts: counts, assignees: uniqueAssignees};
    }, [tasks]);

    const handleStatusChange = useCallback(
        (status: FiltersI['status']) => {
            setFilters({...filters, status});
        },
        [filters, setFilters]
    );

    const handlePriorityChange = useCallback(
        (priority: FiltersI['priority']) => {
            setFilters({...filters, priority});
        },
        [filters, setFilters]
    );

    const handleAssigneeChange = useCallback(
        (assignee: FiltersI['assignee']) => {
            setFilters({...filters, assignee});
        },
        [filters, setFilters]
    );

    const handleResetFilters = useCallback(() => {
        resetFilters();
    }, [resetFilters]);

    return {
        assigneeCounts,
        assignees,
        filters,
        handleAssigneeChange,
        handlePriorityChange,
        handleResetFilters,
        handleStatusChange,
        hasActiveFilters,
        priorityCounts,
        taskCounts,
    };
}
