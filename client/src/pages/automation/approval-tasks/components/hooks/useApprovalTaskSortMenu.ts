import {useCallback} from 'react';

import type {SortDirectionType, SortOptionType} from '../../types/types';

export interface UseApprovalTaskSortMenuParamsI {
    sortBy: SortOptionType;
    sortDirection: SortDirectionType;
    onSortChange: (sortBy: SortOptionType, direction: SortDirectionType) => void;
}

export interface UseApprovalTaskSortMenuReturnI {
    handleSortOptionClick: (optionValue: SortOptionType) => void;
}

export function useApprovalTaskSortMenu({
    onSortChange,
    sortBy,
    sortDirection,
}: UseApprovalTaskSortMenuParamsI): UseApprovalTaskSortMenuReturnI {
    const handleSortOptionClick = useCallback(
        (optionValue: SortOptionType) => {
            if (sortBy === optionValue) {
                onSortChange(optionValue, sortDirection === 'asc' ? 'desc' : 'asc');
            } else {
                onSortChange(optionValue, 'asc');
            }
        },
        [sortBy, sortDirection, onSortChange]
    );

    return {
        handleSortOptionClick,
    };
}
