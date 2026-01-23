import Button from '@/components/Button/Button';
import {Calendar} from '@/components/ui/calendar';
import {Input} from '@/components/ui/input';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {format as formatDate} from 'date-fns';
import {Clock} from 'lucide-react';
import {type ChangeEvent, useState} from 'react';

import type {GridRowType} from './types';

interface DateTimeCellRendererProps {
    columnName: string;
    row: {row: GridRowType};
}

export const DateTimeCellRenderer = ({columnName, row: {row}}: DateTimeCellRendererProps) => {
    const raw = (row as Record<string, unknown>)[columnName];
    let text = '';

    if (raw) {
        try {
            const date = new Date(String(raw));

            if (!isNaN(date.getTime())) {
                text = formatDate(date, 'yyyy-MM-dd HH:mm');
            }
        } catch {
            text = String(raw);
        }
    }

    return <span className="text-sm text-foreground">{text}</span>;
};

interface DateTimeEditCellProps {
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

export const DateTimeEditCell = ({columnName, onRowChange, row}: DateTimeEditCellProps) => {
    const [isPopoverOpen, setIsPopoverOpen] = useState(true);
    const [selectedDate, setSelectedDate] = useState<Date>(getInitialDate(row, columnName) ?? new Date());

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

    return (
        <Popover onOpenChange={handleOpenChange} open={isPopoverOpen}>
            <PopoverTrigger asChild>
                <Button className="h-7 w-full justify-start gap-2 px-2 text-xs" variant="outline">
                    <Clock className="h-3.5 w-3.5" />

                    {selectedDate ? formatDate(selectedDate, 'yyyy-MM-dd HH:mm') : 'Pick date & time'}
                </Button>
            </PopoverTrigger>

            <PopoverContent align="start" className="p-3" side="bottom">
                <div className="flex flex-col gap-3">
                    <Calendar mode="single" onSelect={handleDateSelect} selected={selectedDate} />

                    <div className="flex items-center gap-2">
                        <Input
                            className="h-8 w-24"
                            onChange={handleTimeChange}
                            step="60"
                            type="time"
                            value={`${formattedHours}:${formattedMinutes}`}
                        />

                        <Button className="h-8" onClick={handleCommit} size="sm">
                            Save
                        </Button>
                    </div>
                </div>
            </PopoverContent>
        </Popover>
    );
};

export const createDateTimeCellRenderer = (columnName: string) => {
    return (props: {row: GridRowType}) => <DateTimeCellRenderer columnName={columnName} row={{row: props.row}} />;
};

export const createDateTimeEditCellRenderer = (columnName: string) => {
    return (props: {row: GridRowType; onRowChange: (row: GridRowType, commitChanges?: boolean) => void}) => (
        <DateTimeEditCell columnName={columnName} onRowChange={props.onRowChange} row={props.row} />
    );
};
