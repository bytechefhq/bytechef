import Button from '@/components/Button/Button';
import {Calendar} from '@/components/ui/calendar';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {format as formatDate} from 'date-fns';

import useDateCellEditor, {formatDateValue} from './hooks/useDateCellEditor';

import type {GridRowType} from './types';

interface DateCellRendererProps {
    columnName: string;
    row: {row: GridRowType};
}

export const DateCellRenderer = ({columnName, row: {row}}: DateCellRendererProps) => {
    const cellValue = (row as Record<string, unknown>)[columnName];
    const displayText = formatDateValue(cellValue);

    return <span className="text-sm text-foreground">{displayText}</span>;
};

interface DateEditCellRendererProps {
    columnName: string;
    row: GridRowType;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
}

export const DateEditCellRenderer = ({columnName, onRowChange, row}: DateEditCellRendererProps) => {
    const {handleDateSelect, initialDate, isPopoverOpen, setIsPopoverOpen} = useDateCellEditor({
        columnName,
        onRowChange,
        row,
    });

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
