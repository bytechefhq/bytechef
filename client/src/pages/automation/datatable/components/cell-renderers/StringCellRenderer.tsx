import Button from '@/components/Button/Button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Textarea} from '@/components/ui/textarea';
import {useRef, useState} from 'react';

import type {GridRowType} from './types';

interface StringEditCellProps {
    columnName: string;
    row: GridRowType;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
}

export const StringEditCell = ({columnName, onRowChange, row}: StringEditCellProps) => {
    const initial = String(((row as Record<string, unknown>)[columnName] ?? '') as string);
    const [open, setOpen] = useState(true);
    const [text, setText] = useState<string>(initial);
    const committedRef = useRef(false);

    const doSave = () => {
        committedRef.current = true;
        onRowChange({...row, [columnName]: text}, true);
        setOpen(false);
    };

    const doCancel = () => {
        committedRef.current = true;
        onRowChange(row, true);
        setOpen(false);
    };

    return (
        <Popover
            onOpenChange={(next) => {
                if (!next) {
                    if (!committedRef.current) {
                        onRowChange(row, true);
                    }
                }
                setOpen(next);
            }}
            open={open}
        >
            <PopoverTrigger asChild>
                <Button className="h-7 w-full justify-start px-2 text-xs" variant="outline">
                    Edit text…
                </Button>
            </PopoverTrigger>

            <PopoverContent align="start" className="-mt-9 w-96 p-0" side="bottom">
                <div className="flex flex-col gap-3">
                    <Textarea
                        autoFocus
                        className="min-h-[180px] w-full resize-y"
                        onChange={(event) => setText(event.target.value)}
                        onKeyDown={(event) => {
                            if (event.key === 'Enter' && event.shiftKey) {
                                event.stopPropagation();
                                return;
                            }

                            if (event.key === 'Enter') {
                                event.preventDefault();
                                event.stopPropagation();
                                doSave();
                                return;
                            }

                            if (event.key === 'Escape') {
                                event.stopPropagation();
                                doCancel();
                                return;
                            }
                        }}
                        placeholder={`Enter ${columnName}`}
                        rows={10}
                        value={text}
                    />
                </div>
            </PopoverContent>
        </Popover>
    );
};

export const createStringEditCellRenderer = (columnName: string) => {
    return (props: {row: GridRowType; onRowChange: (row: GridRowType, commitChanges?: boolean) => void}) => (
        <StringEditCell columnName={columnName} onRowChange={props.onRowChange} row={props.row} />
    );
};
