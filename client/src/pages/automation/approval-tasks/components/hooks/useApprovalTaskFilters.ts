import {useCallback, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useApprovalTasksStore} from '../../stores/useApprovalTasksStore';

import type {ApprovalTaskCountsI, FiltersI, PriorityCountsI} from '../../types/types';

export interface UseApprovalTaskFiltersReturnI {
    assigneeCounts: Record<string, number>;
    assignees: string[];
    filters: FiltersI;
    handleAssigneeChange: (assignee: FiltersI['assignee']) => void;
    handlePriorityChange: (priority: FiltersI['priority']) => void;
    handleResetFilters: () => void;
    handleStatusChange: (status: FiltersI['status']) => void;
    approvalTaskCounts: ApprovalTaskCountsI;
    hasActiveFilters: boolean;
    priorityCounts: PriorityCountsI;
}

export function useApprovalTaskFilters(): UseApprovalTaskFiltersReturnI {
    const {approvalTasks, filters, resetFilters, setFilters} = useApprovalTasksStore(
        useShallow((state) => ({
            approvalTasks: state.approvalTasks,
            filters: state.filters,
            resetFilters: state.resetFilters,
            setFilters: state.setFilters,
        }))
    );

    const hasActiveFilters = filters.status !== 'all' || filters.priority !== 'all' || filters.assignee !== 'all';

    const approvalTaskCounts = useMemo(
        (): ApprovalTaskCountsI => ({
            all: approvalTasks.length,
            completed: approvalTasks.filter((approvalTask) => approvalTask.status === 'completed').length,
            'in-progress': approvalTasks.filter((approvalTask) => approvalTask.status === 'in-progress').length,
            open: approvalTasks.filter((approvalTask) => approvalTask.status === 'open').length,
        }),
        [approvalTasks]
    );

    const priorityCounts = useMemo(
        (): PriorityCountsI => ({
            all: approvalTasks.length,
            high: approvalTasks.filter((approvalTask) => approvalTask.priority === 'high').length,
            low: approvalTasks.filter((approvalTask) => approvalTask.priority === 'low').length,
            medium: approvalTasks.filter((approvalTask) => approvalTask.priority === 'medium').length,
        }),
        [approvalTasks]
    );

    const {assigneeCounts, assignees} = useMemo(() => {
        const uniqueAssignees = [...new Set(approvalTasks.map((approvalTask) => approvalTask.assignee))];
        const counts: Record<string, number> = {
            all: approvalTasks.length,
        };

        uniqueAssignees.forEach((assignee) => {
            counts[assignee] = approvalTasks.filter((approvalTask) => approvalTask.assignee === assignee).length;
        });

        return {assigneeCounts: counts, assignees: uniqueAssignees};
    }, [approvalTasks]);

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
        approvalTaskCounts,
        assigneeCounts,
        assignees,
        filters,
        handleAssigneeChange,
        handlePriorityChange,
        handleResetFilters,
        handleStatusChange,
        hasActiveFilters,
        priorityCounts,
    };
}
