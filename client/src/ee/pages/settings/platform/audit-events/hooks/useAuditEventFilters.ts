import {useCallback, useEffect, useState} from 'react';

import {AUDIT_EVENT_DATE_RANGE_PRESETS} from '../constants';
import {AuditEventDateRangePresetType, AuditEventFiltersI} from '../types';

const CLEARED_FILTERS: AuditEventFiltersI = {dateRange: 'all'};

const TYPED_FILTER_DEBOUNCE_MILLISECONDS = 300;

const toStartOfDay = (date: Date) =>
    new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0).getTime();

const toEndOfDay = (date: Date) =>
    new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999).getTime();

export const useAuditEventFilters = () => {
    const [dataSearchInput, setDataSearchInput] = useState('');
    const [filters, setFilters] = useState<AuditEventFiltersI>(CLEARED_FILTERS);
    const [page, setPage] = useState(0);
    const [principalInput, setPrincipalInput] = useState('');

    const hasActiveFilters = !!(
        filters.dataSearch ||
        filters.eventType ||
        filters.fromDate ||
        filters.principal ||
        filters.toDate
    );

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

    const handleClearDataSearch = useCallback(() => {
        setDataSearchInput('');

        updateFilters((previousFilters) => ({...previousFilters, dataSearch: undefined}));
    }, [updateFilters]);

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

    const handleClearPrincipal = useCallback(() => {
        setPrincipalInput('');

        updateFilters((previousFilters) => ({...previousFilters, principal: undefined}));
    }, [updateFilters]);

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

    const handleResetFilters = useCallback(() => {
        setDataSearchInput('');
        setPrincipalInput('');

        updateFilters(() => CLEARED_FILTERS);
    }, [updateFilters]);

    useEffect(() => {
        const dataSearch = dataSearchInput.trim() || undefined;
        const principal = principalInput.trim() || undefined;

        if (dataSearch === filters.dataSearch && principal === filters.principal) {
            return;
        }

        const timeoutId = window.setTimeout(
            () => updateFilters((previousFilters) => ({...previousFilters, dataSearch, principal})),
            TYPED_FILTER_DEBOUNCE_MILLISECONDS
        );

        return () => window.clearTimeout(timeoutId);
    }, [dataSearchInput, filters.dataSearch, filters.principal, principalInput, updateFilters]);

    return {
        dataSearchInput,
        filters,
        handleClearDataSearch,
        handleClearDateRange,
        handleClearEventType,
        handleClearFromDate,
        handleClearPrincipal,
        handleClearToDate,
        handleCustomFromDateChange,
        handleCustomToDateChange,
        handleDateRangePresetChange,
        handleEventTypeChange,
        handleResetFilters,
        hasActiveFilters,
        page,
        principalInput,
        setDataSearchInput,
        setPage,
        setPrincipalInput,
    };
};
