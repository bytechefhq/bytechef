import Button from '@/components/Button/Button';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Textarea} from '@/components/ui/textarea';

import useStringCellEditor from './hooks/useStringCellEditor';

import type {GridRowType} from './types';

const STRING_EDITOR_MIN_HEIGHT = 180;

interface StringEditCellProps {
    columnName: string;
    row: GridRowType;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
}

export const StringEditCell = ({columnName, onRowChange, row}: StringEditCellProps) => {
    const {handleKeyDown, handleOpenChange, handleTextChange, isOpen, text} = useStringCellEditor({
        columnName,
        onRowChange,
        row,
    });

    return (
        <Popover onOpenChange={handleOpenChange} open={isOpen}>
            <PopoverTrigger asChild>
                <Button className="h-7 w-full justify-start px-2 text-xs" variant="outline">
                    Edit text…
                </Button>
            </PopoverTrigger>

            <PopoverContent align="start" className="-mt-9 w-96 p-0" side="bottom">
                <div className="flex flex-col gap-3">
                    <Textarea
                        autoFocus
                        className="w-full resize-y"
                        onChange={handleTextChange}
                        onKeyDown={handleKeyDown}
                        placeholder={`Enter ${columnName}`}
                        rows={10}
                        style={{minHeight: `${STRING_EDITOR_MIN_HEIGHT}px`}}
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
