import {useCallback} from 'react';

import type {SortDirectionType, SortOptionType} from '../../types/types';

export interface UseTaskSortMenuParamsI {
    sortBy: SortOptionType;
    sortDirection: SortDirectionType;
    onSortChange: (sortBy: SortOptionType, direction: SortDirectionType) => void;
}

export interface UseTaskSortMenuReturnI {
    handleSortOptionClick: (optionValue: SortOptionType) => void;
}

export function useTaskSortMenu({onSortChange, sortBy, sortDirection}: UseTaskSortMenuParamsI): UseTaskSortMenuReturnI {
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
