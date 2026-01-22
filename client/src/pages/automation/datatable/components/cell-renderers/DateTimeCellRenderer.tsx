import Button from '@/components/Button/Button';
import {Calendar} from '@/components/ui/calendar';
import {Input} from '@/components/ui/input';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {format as formatDate} from 'date-fns';
import {Clock} from 'lucide-react';
import {useState} from 'react';

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

export const DateTimeEditCell = ({columnName, onRowChange, row}: DateTimeEditCellProps) => {
    const [open, setOpen] = useState(true);
    const raw = (row as Record<string, unknown>)[columnName];

    let existing: Date | undefined;

    if (raw) {
        const date = new Date(String(raw));

        if (!isNaN(date.getTime())) {
            existing = date;
        }
    }

    const [temp, setTemp] = useState<Date>(existing ?? new Date());
    const hours = String(temp.getHours()).padStart(2, '0');
    const minutes = String(temp.getMinutes()).padStart(2, '0');

    const onCommit = (date: Date) => {
        const value = formatDate(date, 'yyyy-MM-dd HH:mm:ss');

        onRowChange({...row, [columnName]: value}, true);
        setOpen(false);
    };

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild>
                <Button className="h-7 w-full justify-start gap-2 px-2 text-xs" variant="outline">
                    <Clock className="h-3.5 w-3.5" />

                    {existing ? formatDate(existing, 'yyyy-MM-dd HH:mm') : 'Pick date & time'}
                </Button>
            </PopoverTrigger>

            <PopoverContent align="start" className="p-3" side="bottom">
                <div className="flex flex-col gap-2">
                    <Calendar
                        mode="single"
                        onSelect={(date) => {
                            if (!date) return;

                            const newDate = new Date(temp);

                            newDate.setFullYear(date.getFullYear(), date.getMonth(), date.getDate());
                            setTemp(newDate);
                            onCommit(newDate);
                        }}
                        selected={existing}
                    />

                    <div className="flex items-center gap-2">
                        <Input
                            className="h-8 w-24"
                            onChange={(event) => {
                                const [hour, minute] = event.target.value.split(':');
                                const newDate = new Date(temp);
                                const parsedHours = Math.min(23, Math.max(0, Number(hour)));
                                const parsedMinutes = Math.min(59, Math.max(0, Number(minute)));

                                newDate.setHours(parsedHours);
                                newDate.setMinutes(parsedMinutes);
                                setTemp(newDate);
                                onCommit(newDate);
                            }}
                            step="60"
                            type="time"
                            value={`${hours}:${minutes}`}
                        />
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
