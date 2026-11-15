import {DataTableLinkOption, useDataTableLinkOptionsQuery} from '@/shared/middleware/graphql';
import {useRef, useState} from 'react';
import {useDebouncedCallback} from 'use-debounce';

import type {GridRowType} from '..';

const LINK_OPTIONS_LIMIT = 50;
const SEARCH_DEBOUNCE_MS = 250;

interface UseLinkCellEditorProps {
    columnName: string;
    environmentId: string;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
    row: GridRowType;
    tableId: string;
}

interface UseLinkCellEditorI {
    handleCancel: () => void;
    handleClear: () => void;
    handleOpenChange: (isOpen: boolean) => void;
    handleSearchChange: (value: string) => void;
    handleSelect: (option: DataTableLinkOption) => void;
    isLoading: boolean;
    isOpen: boolean;
    options: DataTableLinkOption[];
    search: string;
}

export default function useLinkCellEditor({
    columnName,
    environmentId,
    onRowChange,
    row,
    tableId,
}: UseLinkCellEditorProps): UseLinkCellEditorI {
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [isOpen, setIsOpen] = useState(true);
    const [originalRow] = useState(row);
    const [search, setSearch] = useState('');

    const committedRef = useRef(false);

    const {data, isLoading} = useDataTableLinkOptionsQuery({
        columnName,
        environmentId,
        limit: LINK_OPTIONS_LIMIT,
        search: debouncedSearch || undefined,
        tableId,
    });

    const updateDebouncedSearch = useDebouncedCallback((value: string) => {
        setDebouncedSearch(value);
    }, SEARCH_DEBOUNCE_MS);

    const commit = (nextRow: GridRowType) => {
        committedRef.current = true;

        onRowChange(nextRow, true);
        setIsOpen(false);
    };

    const handleCancel = () => {
        if (committedRef.current) {
            return;
        }

        commit(originalRow);
    };

    const handleClear = () => {
        commit({...row, [columnName]: null});
    };

    const handleSelect = (option: DataTableLinkOption) => {
        commit({...row, [columnName]: option.id});
    };

    const handleOpenChange = (openState: boolean) => {
        if (!openState) {
            handleCancel();

            return;
        }

        setIsOpen(openState);
    };

    const handleSearchChange = (value: string) => {
        setSearch(value);

        updateDebouncedSearch(value);
    };

    return {
        handleCancel,
        handleClear,
        handleOpenChange,
        handleSearchChange,
        handleSelect,
        isLoading,
        isOpen,
        options: data?.dataTableLinkOptions ?? [],
        search,
    };
}
