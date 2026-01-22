import {type ChangeEvent, type FocusEvent, type KeyboardEvent, useState} from 'react';

import type {GridRowType} from '..';

interface UseNumberCellEditorProps {
    columnName: string;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
    row: GridRowType;
}

interface UseNumberCellEditorI {
    handleBlur: (event: FocusEvent<HTMLInputElement>) => void;
    handleChange: (event: ChangeEvent<HTMLInputElement>) => void;
    handleKeyDown: (event: KeyboardEvent<HTMLInputElement>) => void;
    inputValue: string;
}

export default function useNumberCellEditor({
    columnName,
    onRowChange,
    row,
}: UseNumberCellEditorProps): UseNumberCellEditorI {
    const cellValue = (row as Record<string, unknown>)[columnName];
    const initialValue = cellValue === null || cellValue === undefined ? '' : String(cellValue);

    const [inputValue, setInputValue] = useState(initialValue);

    const commitValue = (value: string, shouldCommit: boolean) => {
        const trimmedValue = value.trim();

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
        setInputValue(event.target.value);
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        if (event.key === 'Enter') {
            commitValue(inputValue, true);
        }
    };

    return {
        handleBlur,
        handleChange,
        handleKeyDown,
        inputValue,
    };
}
