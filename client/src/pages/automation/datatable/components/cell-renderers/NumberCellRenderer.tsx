import {Input} from '@/components/ui/input';
import {type ChangeEvent, type FocusEvent, type KeyboardEvent} from 'react';

import type {GridRowType} from './types';

interface NumberEditCellProps {
    columnName: string;
    row: GridRowType;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
}

export const NumberEditCell = ({columnName, onRowChange, row}: NumberEditCellProps) => {
    const cellValue = (row as Record<string, unknown>)[columnName];
    const displayValue = cellValue === null || cellValue === undefined ? '' : String(cellValue);

    const commitValue = (inputValue: string, shouldCommit: boolean) => {
        const trimmedValue = inputValue.trim();

        if (trimmedValue === '') {
            onRowChange({...row, [columnName]: null}, shouldCommit);

            return;
        }

        const numericValue = Number(trimmedValue);

        if (Number.isNaN(numericValue)) {
            return;
        }

        onRowChange({...row, [columnName]: numericValue}, shouldCommit);
    };

    const handleBlur = (event: FocusEvent<HTMLInputElement>) => {
        commitValue(event.target.value, true);
    };

    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
        commitValue(event.target.value, false);
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'Enter') {
            const inputElement = event.target as HTMLInputElement;

            commitValue(inputElement.value, true);
        }
    };

    return (
        <Input
            autoFocus
            className="h-7"
            inputMode="decimal"
            onBlur={handleBlur}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            type="number"
            value={displayValue}
        />
    );
};

export const createNumberEditCellRenderer = (columnName: string) => {
    return (props: {row: GridRowType; onRowChange: (row: GridRowType, commitChanges?: boolean) => void}) => (
        <NumberEditCell columnName={columnName} onRowChange={props.onRowChange} row={props.row} />
    );
};
