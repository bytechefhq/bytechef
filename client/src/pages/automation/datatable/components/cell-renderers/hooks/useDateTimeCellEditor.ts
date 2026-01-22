import {format as formatDate} from 'date-fns';
import {type ChangeEvent, useState} from 'react';

import type {GridRowType} from '..';

interface UseDateTimeCellEditorProps {
    columnName: string;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
    row: GridRowType;
}

interface UseDateTimeCellEditorI {
    displayText: string;
    formattedHours: string;
    formattedMinutes: string;
    handleCommit: () => void;
    handleDateSelect: (calendarDate: Date | undefined) => void;
    handleOpenChange: (isOpen: boolean) => void;
    handleTimeChange: (event: ChangeEvent<HTMLInputElement>) => void;
    isPopoverOpen: boolean;
    selectedDate: Date;
}

const parseDateTime = (value: unknown): Date | undefined => {
    if (!value) return undefined;

    const date = new Date(String(value));

    return isNaN(date.getTime()) ? undefined : date;
};

const formatDateTimeValue = (value: unknown): string => {
    if (!value) return '';

    try {
        const date = new Date(String(value));

        if (!isNaN(date.getTime())) {
            return formatDate(date, 'yyyy-MM-dd HH:mm');
        }
    } catch {
        return String(value);
    }

    return '';
};

export default function useDateTimeCellEditor({
    columnName,
    onRowChange,
    row,
}: UseDateTimeCellEditorProps): UseDateTimeCellEditorI {
    const cellValue = (row as Record<string, unknown>)[columnName];

    const [isPopoverOpen, setIsPopoverOpen] = useState(true);
    const [selectedDate, setSelectedDate] = useState<Date>(parseDateTime(cellValue) ?? new Date());

    const displayText = formatDateTimeValue(cellValue);
    const formattedHours = String(selectedDate.getHours()).padStart(2, '0');
    const formattedMinutes = String(selectedDate.getMinutes()).padStart(2, '0');

    const handleCommit = () => {
        const formattedValue = formatDate(selectedDate, 'yyyy-MM-dd HH:mm:ss');

        onRowChange({...row, [columnName]: formattedValue}, true);
        setIsPopoverOpen(false);
    };

    const handleOpenChange = (isOpen: boolean) => {
        if (!isOpen) {
            handleCommit();
        }

        setIsPopoverOpen(isOpen);
    };

    const handleTimeChange = (event: ChangeEvent<HTMLInputElement>) => {
        const [hour, minute] = event.target.value.split(':');
        const updatedDate = new Date(selectedDate);

        const clampedHours = Math.min(23, Math.max(0, Number(hour)));
        const clampedMinutes = Math.min(59, Math.max(0, Number(minute)));

        updatedDate.setHours(clampedHours);
        updatedDate.setMinutes(clampedMinutes);

        setSelectedDate(updatedDate);
    };

    const handleDateSelect = (calendarDate: Date | undefined) => {
        if (!calendarDate) return;

        const updatedDate = new Date(selectedDate);

        updatedDate.setFullYear(calendarDate.getFullYear(), calendarDate.getMonth(), calendarDate.getDate());

        setSelectedDate(updatedDate);
    };

    return {
        displayText,
        formattedHours,
        formattedMinutes,
        handleCommit,
        handleDateSelect,
        handleOpenChange,
        handleTimeChange,
        isPopoverOpen,
        selectedDate,
    };
}

export {formatDateTimeValue, parseDateTime};
