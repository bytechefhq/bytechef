import Button from '@/components/Button/Button';
import {Calendar} from '@/components/ui/calendar';
import {Input} from '@/components/ui/input';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {format as formatDate} from 'date-fns';
import {Clock} from 'lucide-react';

import useDateTimeCellEditor, {formatDateTimeValue} from './hooks/useDateTimeCellEditor';

import type {GridRowType} from './types';

interface DateTimeCellRendererProps {
    columnName: string;
    row: {row: GridRowType};
}

export const DateTimeCellRenderer = ({columnName, row: {row}}: DateTimeCellRendererProps) => {
    const cellValue = (row as Record<string, unknown>)[columnName];
    const displayText = formatDateTimeValue(cellValue);

    return <span className="text-sm text-foreground">{displayText}</span>;
};

interface DateTimeEditCellProps {
    columnName: string;
    row: GridRowType;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
}

export const DateTimeEditCell = ({columnName, onRowChange, row}: DateTimeEditCellProps) => {
    const {
        formattedHours,
        formattedMinutes,
        handleCommit,
        handleDateSelect,
        handleOpenChange,
        handleTimeChange,
        isPopoverOpen,
        selectedDate,
    } = useDateTimeCellEditor({
        columnName,
        onRowChange,
        row,
    });

    return (
        <Popover onOpenChange={handleOpenChange} open={isPopoverOpen}>
            <PopoverTrigger asChild>
                <Button className="h-7 w-full justify-start gap-2 px-2 text-xs" variant="outline">
                    <Clock className="size-3.5" />

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
