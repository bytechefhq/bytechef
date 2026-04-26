import {useUpdateApprovalTaskMutation} from '@/shared/middleware/graphql';
import {MouseEvent, useCallback, useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useApprovalTasksStore} from '../../stores/useApprovalTasksStore';
import {getHeaderText, sortApprovalTasks, toServerPriority, toServerStatus} from '../../utils/approval-task-utils';

import type {ApprovalTaskI, SortDirectionType, SortOptionType} from '../../types/types';

export interface UseApprovalTaskListReturnI {
    emptyStateMessage: string;
    filteredApprovalTasks: ApprovalTaskI[];
    handleClearFilters: () => void;
    handleSelectApprovalTask: (approvalTaskId: string) => void;
    handleSortChange: (sortBy: SortOptionType, direction: SortDirectionType) => void;
    handleStatusToggle: (approvalTaskId: string, event: MouseEvent) => void;
    headerText: string;
    sortBy: SortOptionType;
    sortDirection: SortDirectionType;
    totalApprovalTaskCount: number;
}

const cycleStatus = (status: ApprovalTaskI['status']): ApprovalTaskI['status'] => {
    switch (status) {
        case 'open':
            return 'in-progress';
        case 'in-progress':
            return 'open';
        default:
            return 'open';
    }
};

export function useApprovalTaskList(): UseApprovalTaskListReturnI {
    const [sortBy, setSortBy] = useState<SortOptionType>('dueDate');
    const [sortDirection, setSortDirection] = useState<SortDirectionType>('asc');

    const {
        approvalTasks,
        filters,
        hasActiveFilters,
        resetFilters,
        searchQuery,
        setSearchQuery,
        setSelectedApprovalTaskId,
        updateApprovalTaskInStore,
    } = useApprovalTasksStore(
        useShallow((state) => ({
            approvalTasks: state.approvalTasks,
            filters: state.filters,
            hasActiveFilters: state.hasActiveFilters(),
            resetFilters: state.resetFilters,
            searchQuery: state.searchQuery,
            setSearchQuery: state.setSearchQuery,
            setSelectedApprovalTaskId: state.setSelectedApprovalTaskId,
            updateApprovalTaskInStore: state.updateApprovalTask,
        }))
    );

    const updateApprovalTaskMutation = useUpdateApprovalTaskMutation({
        onError: (error) => {
            console.error('Error updating approval task:', error);
        },
    });

    const filteredApprovalTasks = useMemo(() => {
        const filtered = approvalTasks.filter((approvalTask) => {
            const matchesSearch =
                approvalTask.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                approvalTask.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
                approvalTask.assignee.toLowerCase().includes(searchQuery.toLowerCase());

            const matchesStatus = filters.status === 'all' || approvalTask.status === filters.status;
            const matchesPriority = filters.priority === 'all' || approvalTask.priority === filters.priority;
            const matchesAssignee = filters.assignee === 'all' || approvalTask.assignee === filters.assignee;

            return matchesSearch && matchesStatus && matchesPriority && matchesAssignee;
        });

        return sortApprovalTasks(filtered, sortBy, sortDirection);
    }, [approvalTasks, searchQuery, filters, sortBy, sortDirection]);

    const emptyStateMessage = useMemo(() => {
        if (searchQuery) {
            return `No approval tasks match "${searchQuery}"`;
        }

        if (hasActiveFilters) {
            return 'No approval tasks match the current filters';
        }

        return 'You will see here approval tasks assigned to you';
    }, [searchQuery, hasActiveFilters]);

    const headerText = getHeaderText(filters);

    const handleClearFilters = useCallback(() => {
        setSearchQuery('');
        resetFilters();
    }, [setSearchQuery, resetFilters]);

    const handleSelectApprovalTask = useCallback(
        (approvalTaskId: string) => {
            setSelectedApprovalTaskId(approvalTaskId);
        },
        [setSelectedApprovalTaskId]
    );

    const handleSortChange = useCallback((newSortBy: SortOptionType, direction: SortDirectionType) => {
        setSortBy(newSortBy);
        setSortDirection(direction);
    }, []);

    const handleStatusToggle = useCallback(
        (approvalTaskId: string, event: MouseEvent) => {
            event.stopPropagation();

            const target = approvalTasks.find((approvalTask) => approvalTask.id === approvalTaskId);

            if (!target) {
                return;
            }

            const next: ApprovalTaskI = {...target, status: cycleStatus(target.status)};

            updateApprovalTaskInStore(next);

            updateApprovalTaskMutation.mutate(
                {
                    approvalTask: {
                        assigneeId: next.assigneeId,
                        description: next.description,
                        id: next.id,
                        name: next.title,
                        priority: toServerPriority(next.priority),
                        status: toServerStatus(next.status),
                        version: next.version,
                    },
                },
                {
                    onSuccess: (data) => {
                        updateApprovalTaskInStore({
                            ...next,
                            version: data.updateApprovalTask?.version ?? next.version,
                        });
                    },
                }
            );
        },
        [approvalTasks, updateApprovalTaskInStore, updateApprovalTaskMutation]
    );

    return {
        emptyStateMessage,
        filteredApprovalTasks,
        handleClearFilters,
        handleSelectApprovalTask,
        handleSortChange,
        handleStatusToggle,
        headerText,
        sortBy,
        sortDirection,
        totalApprovalTaskCount: approvalTasks.length,
    };
}
