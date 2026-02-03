import {useCallback} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useTasksStore} from '../../stores/useTasksStore';

import type {FiltersI} from '../../types/types';

export interface UseTaskActiveFilterBadgesReturnI {
    filters: FiltersI;
    handleClearAssigneeFilter: () => void;
    handleClearPriorityFilter: () => void;
    handleClearStatusFilter: () => void;
    handleResetFilters: () => void;
    hasActiveFilters: boolean;
}

export function useTaskActiveFilterBadges(): UseTaskActiveFilterBadgesReturnI {
    const {filters, hasActiveFilters, resetFilters, setFilters} = useTasksStore(
        useShallow((state) => ({
            filters: state.filters,
            hasActiveFilters: state.hasActiveFilters(),
            resetFilters: state.resetFilters,
            setFilters: state.setFilters,
        }))
    );

    const handleClearStatusFilter = useCallback(() => {
        setFilters((prevFilters) => ({...prevFilters, status: 'all'}));
    }, [setFilters]);

    const handleClearPriorityFilter = useCallback(() => {
        setFilters((prevFilters) => ({...prevFilters, priority: 'all'}));
    }, [setFilters]);

    const handleClearAssigneeFilter = useCallback(() => {
        setFilters((prevFilters) => ({...prevFilters, assignee: 'all'}));
    }, [setFilters]);

    const handleResetFilters = useCallback(() => {
        resetFilters();
    }, [resetFilters]);

    return {
        filters,
        handleClearAssigneeFilter,
        handleClearPriorityFilter,
        handleClearStatusFilter,
        handleResetFilters,
        hasActiveFilters,
    };
}
