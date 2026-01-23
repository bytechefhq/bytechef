import {Input} from '@/components/ui/input';
import {type ChangeEvent, type FocusEvent, type KeyboardEvent} from 'react';

import type {GridRowType} from './types';

interface NumberEditCellProps {
    columnName: string;
    row: GridRowType;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
}

export const NumberEditCell = ({columnName, onRowChange, row}: NumberEditCellProps) => {
    const raw = (row as Record<string, unknown>)[columnName];
    const value = raw === null || raw === undefined ? '' : String(raw);

    const commit = (inputValue: string, commitChanges: boolean) => {
        const trimmed = inputValue.trim();

        // Use null for empty input, otherwise convert to a number.
        if (trimmed === '') {
            onRowChange({...row, [columnName]: null}, commitChanges);

            return;
        }

        const parsed = Number(trimmed);

        // If the value cannot be parsed as a number, do not commit the change.
        if (Number.isNaN(parsed)) {
            return;
        }

        onRowChange({...row, [columnName]: parsed}, commitChanges);
    };

    const handleBlur = (event: FocusEvent<HTMLInputElement>) => {
        commit(event.target.value, true);
    };

    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
        commit(event.target.value, false);
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'Enter') {
            const target = event.target as HTMLInputElement;

            commit(target.value, true);
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
            value={value}
        />
    );
};

export const createNumberEditCellRenderer = (columnName: string) => {
    return (props: {row: GridRowType; onRowChange: (row: GridRowType, commitChanges?: boolean) => void}) => (
        <NumberEditCell columnName={columnName} onRowChange={props.onRowChange} row={props.row} />
    );
};
