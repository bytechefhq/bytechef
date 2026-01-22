import type {GridRowType} from '..';
import type {Dispatch, SetStateAction} from 'react';

interface UseBooleanCellEditorProps {
    columnName: string;
    environmentId: string;
    row: GridRowType;
    setLocalRows: Dispatch<SetStateAction<GridRowType[]>>;
    tableId: string;
    updateRowMutation: {
        mutate: (params: {
            input: {
                environmentId: string;
                id: string;
                tableId: string;
                values: Record<string, unknown>;
            };
        }) => void;
    };
}

interface UseBooleanCellEditorI {
    checked: boolean;
    handleToggle: (nextValue: boolean) => void;
}

export default function useBooleanCellEditor({
    columnName,
    environmentId,
    row,
    setLocalRows,
    tableId,
    updateRowMutation,
}: UseBooleanCellEditorProps): UseBooleanCellEditorI {
    const rowId = row.id;
    const checked = Boolean((row as Record<string, unknown>)[columnName]);

    const handleToggle = (nextValue: boolean) => {
        setLocalRows((previousRows) =>
            previousRows.map((currentRow) =>
                currentRow.id === rowId ? {...currentRow, [columnName]: nextValue} : currentRow
            )
        );

        updateRowMutation.mutate({
            input: {environmentId, id: rowId, tableId, values: {[columnName]: nextValue}},
        });
    };

    return {
        checked,
        handleToggle,
    };
}
