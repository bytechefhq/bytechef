import type {GridRowType} from '..';
import type {Dispatch, SetStateAction} from 'react';

interface UseBooleanCellEditorProps {
    columnName: string;
    onToggle: (rowId: string, columnName: string, value: boolean) => void;
    row: GridRowType;
    setLocalRows: Dispatch<SetStateAction<GridRowType[]>>;
}

interface UseBooleanCellEditorI {
    checked: boolean;
    handleToggle: (nextValue: boolean) => void;
}

export default function useBooleanCellEditor({
    columnName,
    onToggle,
    row,
    setLocalRows,
}: UseBooleanCellEditorProps): UseBooleanCellEditorI {
    const rowId = row.id;
    const checked = Boolean((row as Record<string, unknown>)[columnName]);

    const handleToggle = (nextValue: boolean) => {
        setLocalRows((previousRows) =>
            previousRows.map((currentRow) =>
                currentRow.id === rowId ? {...currentRow, [columnName]: nextValue} : currentRow
            )
        );

        onToggle(rowId, columnName, nextValue);
    };

    return {
        checked,
        handleToggle,
    };
}
