import {format as formatDate} from 'date-fns';
import {useState} from 'react';

import type {GridRowType} from '..';

interface UseDateCellEditorProps {
    columnName: string;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
    row: GridRowType;
}

interface UseDateCellEditorI {
    displayText: string;
    handleDateSelect: (selectedDate: Date | undefined) => void;
    initialDate: Date | undefined;
    isPopoverOpen: boolean;
    setIsPopoverOpen: (open: boolean) => void;
}

const parseDate = (value: unknown): Date | undefined => {
    if (!value) return undefined;

    const date = new Date(String(value));

    return isNaN(date.getTime()) ? undefined : date;
};

const formatDateValue = (value: unknown): string => {
    if (!value) return '';

    try {
        const date = new Date(String(value));

        if (!isNaN(date.getTime())) {
            return formatDate(date, 'yyyy-MM-dd');
        }
    } catch {
        return String(value);
    }

    return '';
};

export default function useDateCellEditor({columnName, onRowChange, row}: UseDateCellEditorProps): UseDateCellEditorI {
    const [isPopoverOpen, setIsPopoverOpen] = useState(true);

    const cellValue = (row as Record<string, unknown>)[columnName];
    const initialDate = parseDate(cellValue);
    const displayText = formatDateValue(cellValue);

    const handleDateSelect = (selectedDate: Date | undefined) => {
        if (!selectedDate) return;

        const formattedValue = formatDate(selectedDate, 'yyyy-MM-dd');

        onRowChange({...row, [columnName]: formattedValue}, true);
        setIsPopoverOpen(false);
    };

    return {
        displayText,
        handleDateSelect,
        initialDate,
        isPopoverOpen,
        setIsPopoverOpen,
    };
}

export {formatDateValue, parseDate};
