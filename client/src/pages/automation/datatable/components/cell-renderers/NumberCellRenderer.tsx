import {Input} from '@/components/ui/input';

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
        const output: unknown = trimmed === '' ? null : trimmed;

        onRowChange({...row, [columnName]: output}, commitChanges);
    };

    return (
        <Input
            autoFocus
            className="h-7"
            inputMode="decimal"
            onBlur={(event) => commit(event.target.value, true)}
            onChange={(event) => commit(event.target.value, false)}
            onKeyDown={(event) => {
                if (event.key === 'Enter') {
                    const target = event.target as HTMLInputElement;

                    commit(target.value, true);
                }
            }}
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
