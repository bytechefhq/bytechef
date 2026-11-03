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
    handleOpenChange: (open: boolean) => void;
    initialDate: Date | undefined;
    isPopoverOpen: boolean;
}

const CALENDAR_DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})/;

const parseDate = (value: unknown): Date | undefined => {
    if (value === null || value === undefined || value === '') return undefined;

    if (typeof value === 'number') {
        const utcDate = new Date(value);

        if (isNaN(utcDate.getTime())) return undefined;

        return new Date(utcDate.getUTCFullYear(), utcDate.getUTCMonth(), utcDate.getUTCDate());
    }

    const calendarDateMatch = CALENDAR_DATE_PATTERN.exec(String(value));

    if (!calendarDateMatch) return undefined;

    const [, year, month, day] = calendarDateMatch;
    const date = new Date(Number(year), Number(month) - 1, Number(day));

    return isNaN(date.getTime()) ? undefined : date;
};

const formatDateValue = (value: unknown): string => {
    const date = parseDate(value);

    return date ? formatDate(date, 'yyyy-MM-dd') : '';
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

    const handleOpenChange = (open: boolean) => {
        setIsPopoverOpen(open);
    };

    return {
        displayText,
        handleDateSelect,
        handleOpenChange,
        initialDate,
        isPopoverOpen,
    };
}

export {formatDateValue, parseDate};
