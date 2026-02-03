import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useTasksStore} from '../../stores/useTasksStore';
import {highlightText} from '../../utils/task-utils';

import type React from 'react';

export interface SuggestionI {
    highlighted: React.ReactNode;
    text: string;
}

export interface UseTaskSearchReturnI {
    // State
    searchInputRef: React.RefObject<HTMLInputElement>;
    searchQuery: string;
    selectedSuggestionIndex: number;
    shortcutText: string;
    showSuggestions: boolean;
    suggestions: SuggestionI[];

    // Handlers
    handleInputFocus: () => void;
    handleKeyDown: (event: React.KeyboardEvent) => void;
    handleSearchChange: (value: string) => void;
    handleSuggestionClick: (suggestion: string) => void;
}

export function useTaskSearch(): UseTaskSearchReturnI {
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [selectedSuggestionIndex, setSelectedSuggestionIndex] = useState(-1);
    const searchInputRef = useRef<HTMLInputElement>(null);

    const {filters, searchQuery, setSearchQuery, tasks} = useTasksStore(
        useShallow((state) => ({
            filters: state.filters,
            searchQuery: state.searchQuery,
            setSearchQuery: state.setSearchQuery,
            tasks: state.tasks,
        }))
    );

    const isMac = typeof navigator !== 'undefined' ? /Mac|iPod|iPhone|iPad/.test(navigator.platform) : false;
    const shortcutText = isMac ? '⌘K' : 'Ctrl+K';

    // Compute suggestions based on search query and filters
    const suggestions = useMemo(() => {
        if (!searchQuery.trim()) {
            return [];
        }

        const tasksMatchingFilters = tasks.filter((task) => {
            const matchesStatus = filters.status === 'all' || task.status === filters.status;
            const matchesPriority = filters.priority === 'all' || task.priority === filters.priority;
            const matchesAssignee = filters.assignee === 'all' || task.assignee === filters.assignee;

            return matchesStatus && matchesPriority && matchesAssignee;
        });

        const titleSuggestions = tasksMatchingFilters
            .filter(
                (task) =>
                    task.title.toLowerCase().includes(searchQuery.toLowerCase()) &&
                    task.title.toLowerCase() !== searchQuery.toLowerCase()
            )
            .map((task) => task.title);

        const assigneeSuggestions = tasksMatchingFilters
            .filter(
                (task) =>
                    task.assignee.toLowerCase().includes(searchQuery.toLowerCase()) &&
                    task.assignee.toLowerCase() !== searchQuery.toLowerCase()
            )
            .map((task) => task.assignee);

        const allSuggestions = [...titleSuggestions, ...assigneeSuggestions].slice(0, 5);
        const uniqueSuggestions = [...new Set(allSuggestions)];

        return uniqueSuggestions.map((suggestion) => ({
            highlighted: highlightText(suggestion, searchQuery),
            text: suggestion,
        }));
    }, [searchQuery, tasks, filters]);

    // Keyboard shortcut to focus search (Cmd+K on Mac, Ctrl+K on Windows/Linux)
    useEffect(() => {
        const handleGlobalKeyDown = (event: KeyboardEvent) => {
            if ((event.metaKey || event.ctrlKey) && event.key === 'k') {
                event.preventDefault();
                searchInputRef.current?.focus();
            }
        };

        document.addEventListener('keydown', handleGlobalKeyDown);

        return () => document.removeEventListener('keydown', handleGlobalKeyDown);
    }, []);

    // Close suggestions when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
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
        (event: React.KeyboardEvent) => {
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
        shortcutText,
        showSuggestions,
        suggestions,
    };
}
