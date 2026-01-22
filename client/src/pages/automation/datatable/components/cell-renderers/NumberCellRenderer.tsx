import {Input} from '@/components/ui/input';

import useNumberCellEditor from './hooks/useNumberCellEditor';

import type {GridRowType} from './types';

interface NumberEditCellProps {
    columnName: string;
    row: GridRowType;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
}

export const NumberEditCell = ({columnName, onRowChange, row}: NumberEditCellProps) => {
    const {handleBlur, handleChange, handleKeyDown, inputValue} = useNumberCellEditor({
        columnName,
        onRowChange,
        row,
    });

    return (
        <Input
            autoFocus
            className="h-7"
            inputMode="decimal"
            onBlur={handleBlur}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            type="number"
            value={inputValue}
        />
    );
};

export const createNumberEditCellRenderer = (columnName: string) => {
    return (props: {row: GridRowType; onRowChange: (row: GridRowType, commitChanges?: boolean) => void}) => (
        <NumberEditCell columnName={columnName} onRowChange={props.onRowChange} row={props.row} />
    );
};
