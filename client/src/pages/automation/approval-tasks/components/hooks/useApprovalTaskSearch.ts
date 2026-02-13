import {KeyboardEvent, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useApprovalTasksStore} from '../../stores/useApprovalTasksStore';
import {highlightText} from '../../utils/approval-task-utils';

import type {ReactNode, RefObject} from 'react';

export interface SuggestionI {
    highlighted: ReactNode;
    text: string;
}

export interface UseApprovalTaskSearchReturnI {
    handleInputFocus: () => void;
    handleKeyDown: (event: KeyboardEvent) => void;
    handleSearchChange: (value: string) => void;
    handleSuggestionClick: (suggestion: string) => void;
    searchInputRef: RefObject<HTMLInputElement>;
    searchQuery: string;
    selectedSuggestionIndex: number;
    showSuggestions: boolean;
    suggestions: SuggestionI[];
}

export function useApprovalTaskSearch(): UseApprovalTaskSearchReturnI {
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [selectedSuggestionIndex, setSelectedSuggestionIndex] = useState(-1);

    const searchInputRef = useRef<HTMLInputElement>(null);

    const {approvalTasks, filters, searchQuery, setSearchQuery} = useApprovalTasksStore(
        useShallow((state) => ({
            approvalTasks: state.approvalTasks,
            filters: state.filters,
            searchQuery: state.searchQuery,
            setSearchQuery: state.setSearchQuery,
        }))
    );

    const suggestions = useMemo(() => {
        if (!searchQuery.trim()) {
            return [];
        }

        const approvalTasksMatchingFilters = approvalTasks.filter((approvalTask) => {
            const matchesStatus = filters.status === 'all' || approvalTask.status === filters.status;
            const matchesPriority = filters.priority === 'all' || approvalTask.priority === filters.priority;
            const matchesAssignee = filters.assignee === 'all' || approvalTask.assignee === filters.assignee;

            return matchesStatus && matchesPriority && matchesAssignee;
        });

        const titleSuggestions = approvalTasksMatchingFilters
            .filter(
                (approvalTask) =>
                    approvalTask.title.toLowerCase().includes(searchQuery.toLowerCase()) &&
                    approvalTask.title.toLowerCase() !== searchQuery.toLowerCase()
            )
            .map((approvalTask) => approvalTask.title);

        const assigneeSuggestions = approvalTasksMatchingFilters
            .filter(
                (approvalTask) =>
                    approvalTask.assignee.toLowerCase().includes(searchQuery.toLowerCase()) &&
                    approvalTask.assignee.toLowerCase() !== searchQuery.toLowerCase()
            )
            .map((approvalTask) => approvalTask.assignee);

        const allSuggestions = [...titleSuggestions, ...assigneeSuggestions].slice(0, 5);
        const uniqueSuggestions = [...new Set(allSuggestions)];

        return uniqueSuggestions.map((suggestion) => ({
            highlighted: highlightText(suggestion, searchQuery),
            text: suggestion,
        }));
    }, [searchQuery, approvalTasks, filters]);

    // Close suggestions when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: globalThis.MouseEvent) => {
            if (searchInputRef.current && !searchInputRef.current.contains(event.target as Node)) {
                setShowSuggestions(false);
                setSelectedSuggestionIndex(-1);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);

        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleSearchChange = useCallback(
        (value: string) => {
            setSearchQuery(value);
            setShowSuggestions(value.length > 0);
            setSelectedSuggestionIndex(-1);
        },
        [setSearchQuery]
    );

    const handleSuggestionClick = useCallback(
        (suggestion: string) => {
            setSearchQuery(suggestion);
            setShowSuggestions(false);
            setSelectedSuggestionIndex(-1);
            searchInputRef.current?.focus();
        },
        [setSearchQuery]
    );

    const handleKeyDown = useCallback(
        (event: KeyboardEvent) => {
            if (!showSuggestions || suggestions.length === 0) {
                return;
            }

            switch (event.key) {
                case 'ArrowDown':
                    event.preventDefault();
                    setSelectedSuggestionIndex((previousIndex) =>
                        previousIndex < suggestions.length - 1 ? previousIndex + 1 : previousIndex
                    );
                    break;
                case 'ArrowUp':
                    event.preventDefault();
                    setSelectedSuggestionIndex((previousIndex) => (previousIndex > 0 ? previousIndex - 1 : -1));
                    break;
                case 'Enter':
                    event.preventDefault();

                    if (selectedSuggestionIndex >= 0) {
                        handleSuggestionClick(suggestions[selectedSuggestionIndex].text);
                    }
                    break;
                case 'Escape':
                    setShowSuggestions(false);
                    setSelectedSuggestionIndex(-1);
                    break;
            }
        },
        [showSuggestions, suggestions, selectedSuggestionIndex, handleSuggestionClick]
    );

    const handleInputFocus = useCallback(() => {
        if (searchQuery.length > 0) {
            setShowSuggestions(true);
        }
    }, [searchQuery]);

    return {
        handleInputFocus,
        handleKeyDown,
        handleSearchChange,
        handleSuggestionClick,
        searchInputRef,
        searchQuery,
        selectedSuggestionIndex,
        showSuggestions,
        suggestions,
    };
}
