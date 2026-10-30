import {useCallback, useEffect, useState} from 'react';

import {AUDIT_EVENT_DATE_RANGE_PRESETS} from '../constants';
import {AuditEventDateRangePresetType, AuditEventFiltersI} from '../types';

const CLEARED_FILTERS: AuditEventFiltersI = {dateRange: 'all'};

const SEARCH_DEBOUNCE_MILLISECONDS = 300;

const toStartOfDay = (date: Date) =>
    new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0).getTime();

const toEndOfDay = (date: Date) =>
    new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999).getTime();

export const useAuditEventFilters = () => {
    const [filters, setFilters] = useState<AuditEventFiltersI>(CLEARED_FILTERS);
    const [page, setPage] = useState(0);
    const [search, setSearch] = useState<string>();
    const [searchInput, setSearchInput] = useState('');

    const hasActiveFilters = !!(filters.eventType || filters.fromDate || filters.toDate);

    const updateFilters = useCallback((updater: (previousFilters: AuditEventFiltersI) => AuditEventFiltersI) => {
        setFilters(updater);
        setPage(0);
    }, []);

    const updateCustomDateRange = useCallback(
        (changes: Pick<AuditEventFiltersI, 'fromDate'> | Pick<AuditEventFiltersI, 'toDate'>) =>
            updateFilters((previousFilters) => {
                const customFilters =
                    previousFilters.dateRange === 'custom'
                        ? {...previousFilters, ...changes}
                        : {...previousFilters, fromDate: undefined, toDate: undefined, ...changes};

                return {
                    ...customFilters,
                    dateRange: customFilters.fromDate || customFilters.toDate ? 'custom' : 'all',
                };
            }),
        [updateFilters]
    );

    const handleClearDateRange = useCallback(
        () =>
            updateFilters((previousFilters) => ({
                ...previousFilters,
                dateRange: 'all',
                fromDate: undefined,
                toDate: undefined,
            })),
        [updateFilters]
    );

    const handleClearEventType = useCallback(
        () => updateFilters((previousFilters) => ({...previousFilters, eventType: undefined})),
        [updateFilters]
    );

    const handleClearFromDate = useCallback(
        () => updateCustomDateRange({fromDate: undefined}),
        [updateCustomDateRange]
    );

    const handleClearToDate = useCallback(() => updateCustomDateRange({toDate: undefined}), [updateCustomDateRange]);

    const handleCustomFromDateChange = useCallback(
        (date?: Date) => updateCustomDateRange({fromDate: date ? toStartOfDay(date) : undefined}),
        [updateCustomDateRange]
    );

    const handleCustomToDateChange = useCallback(
        (date?: Date) => updateCustomDateRange({toDate: date ? toEndOfDay(date) : undefined}),
        [updateCustomDateRange]
    );

    const handleDateRangePresetChange = useCallback(
        (dateRangePreset: AuditEventDateRangePresetType) => {
            const preset = AUDIT_EVENT_DATE_RANGE_PRESETS.find(
                (dateRangePresetItem) => dateRangePresetItem.value === dateRangePreset
            );

            if (!preset) {
                return;
            }

            updateFilters((previousFilters) => ({
                ...previousFilters,
                dateRange: preset.value,
                fromDate: Date.now() - preset.durationMilliseconds,
                toDate: undefined,
            }));
        },
        [updateFilters]
    );

    const handleEventTypeChange = useCallback(
        (eventType?: string) => updateFilters((previousFilters) => ({...previousFilters, eventType})),
        [updateFilters]
    );

    const handleResetFilters = useCallback(() => updateFilters(() => CLEARED_FILTERS), [updateFilters]);

    useEffect(() => {
        const trimmedSearch = searchInput.trim() || undefined;

        if (trimmedSearch === search) {
            return;
        }

        const timeoutId = window.setTimeout(() => {
            setSearch(trimmedSearch);
            setPage(0);
        }, SEARCH_DEBOUNCE_MILLISECONDS);

        return () => window.clearTimeout(timeoutId);
    }, [search, searchInput]);

    return {
        filters,
        handleClearDateRange,
        handleClearEventType,
        handleClearFromDate,
        handleClearToDate,
        handleCustomFromDateChange,
        handleCustomToDateChange,
        handleDateRangePresetChange,
        handleEventTypeChange,
        handleResetFilters,
        hasActiveFilters,
        page,
        search,
        searchInput,
        setPage,
        setSearchInput,
    };
};
