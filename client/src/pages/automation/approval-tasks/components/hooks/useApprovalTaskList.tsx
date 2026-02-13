import {MouseEvent, useCallback, useMemo, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useApprovalTasksStore} from '../../stores/useApprovalTasksStore';
import {getHeaderText, sortApprovalTasks} from '../../utils/approval-task-utils';

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

export function useApprovalTaskList(): UseApprovalTaskListReturnI {
    const [sortBy, setSortBy] = useState<SortOptionType>('created');
    const [sortDirection, setSortDirection] = useState<SortDirectionType>('desc');

    const {
        approvalTasks,
        cycleApprovalTaskStatus,
        filters,
        hasActiveFilters,
        resetFilters,
        searchQuery,
        setSearchQuery,
        setSelectedApprovalTaskId,
    } = useApprovalTasksStore(
        useShallow((state) => ({
            approvalTasks: state.approvalTasks,
            cycleApprovalTaskStatus: state.cycleApprovalTaskStatus,
            filters: state.filters,
            hasActiveFilters: state.hasActiveFilters(),
            resetFilters: state.resetFilters,
            searchQuery: state.searchQuery,
            setSearchQuery: state.setSearchQuery,
            setSelectedApprovalTaskId: state.setSelectedApprovalTaskId,
        }))
    );

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

            cycleApprovalTaskStatus(approvalTaskId);
        },
        [cycleApprovalTaskStatus]
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
