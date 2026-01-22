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

interface DateEditCellProps {
    columnName: string;
    row: GridRowType;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
}

export const DateEditCell = ({columnName, onRowChange, row}: DateEditCellProps) => {
    const [open, setOpen] = useState(true);
    const raw = (row as Record<string, unknown>)[columnName];
    let initial: Date | undefined;

    if (raw) {
        const date = new Date(String(raw));

        if (!isNaN(date.getTime())) {
            initial = date;
        }
    }

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild>
                <Button className="h-7 w-full justify-start px-2 text-xs" variant="outline">
                    {initial ? formatDate(initial, 'yyyy-MM-dd') : 'Pick a date'}
                </Button>
            </PopoverTrigger>

            <PopoverContent align="start" className="p-2" side="bottom">
                <Calendar
                    mode="single"
                    onSelect={(date) => {
                        if (!date) return;

                        const value = formatDate(date, 'yyyy-MM-dd');

                        onRowChange({...row, [columnName]: value}, true);
                        setOpen(false);
                    }}
                    selected={initial}
                />
            </PopoverContent>
        </Popover>
    );
};

export const createDateCellRenderer = (columnName: string) => {
    return (props: {row: GridRowType}) => <DateCellRenderer columnName={columnName} row={{row: props.row}} />;
};

export const createDateEditCellRenderer = (columnName: string) => {
    return (props: {row: GridRowType; onRowChange: (row: GridRowType, commitChanges?: boolean) => void}) => (
        <DateEditCell columnName={columnName} onRowChange={props.onRowChange} row={props.row} />
    );
};
