import Button from '@/components/Button/Button';
import {Calendar} from '@/components/ui/calendar';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {format as formatDate} from 'date-fns';
import {useState} from 'react';

import type {GridRowType} from './types';

interface DateCellRendererProps {
    columnName: string;
    row: {row: GridRowType};
}

export const DateCellRenderer = ({columnName, row: {row}}: DateCellRendererProps) => {
    const raw = (row as Record<string, unknown>)[columnName];
    let text = '';

    if (raw) {
        try {
            const date = new Date(String(raw));

            if (!isNaN(date.getTime())) {
                text = formatDate(date, 'yyyy-MM-dd');
            }
        } catch {
            text = String(raw);
        }
    }

    return <span className="text-sm text-foreground">{text}</span>;
};

interface DateEditCellRendererProps {
    columnName: string;
    row: GridRowType;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
}

const getInitialDate = (row: GridRowType, columnName: string): Date | undefined => {
    const cellValue = (row as Record<string, unknown>)[columnName];

    if (!cellValue) return undefined;

    const date = new Date(String(cellValue));

    return isNaN(date.getTime()) ? undefined : date;
};

export const DateEditCellRenderer = ({columnName, onRowChange, row}: DateEditCellRendererProps) => {
    const initialDate = getInitialDate(row, columnName);

    const [isPopoverOpen, setIsPopoverOpen] = useState(true);

    const handleDateSelect = (selectedDate: Date | undefined) => {
        if (!selectedDate) return;

        const formattedValue = formatDate(selectedDate, 'yyyy-MM-dd');

        onRowChange({...row, [columnName]: formattedValue}, true);
        setIsPopoverOpen(false);
    };

    return (
        <Popover onOpenChange={setIsPopoverOpen} open={isPopoverOpen}>
            <PopoverTrigger asChild>
                <Button className="h-7 w-full justify-start px-2 text-xs" variant="outline">
                    {initialDate ? formatDate(initialDate, 'yyyy-MM-dd') : 'Pick a date'}
                </Button>
            </PopoverTrigger>

            <PopoverContent align="start" className="p-2" side="bottom">
                <Calendar mode="single" onSelect={handleDateSelect} selected={initialDate} />
            </PopoverContent>
        </Popover>
    );
};

export const createDateCellRenderer = (columnName: string) => {
    return (props: {row: GridRowType}) => <DateCellRenderer columnName={columnName} row={{row: props.row}} />;
};

export const createDateEditCellRenderer = (columnName: string) => {
    return (props: {row: GridRowType; onRowChange: (row: GridRowType, commitChanges?: boolean) => void}) => (
        <DateEditCellRenderer columnName={columnName} onRowChange={props.onRowChange} row={props.row} />
    );
};
