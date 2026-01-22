import {Checkbox} from '@/components/ui/checkbox';

import type {CellRendererProps, GridRowType} from './types';

export const createBooleanCellRenderer = ({
    columnName,
    environmentId,
    setLocalRows,
    tableId,
    updateRowMutation,
}: CellRendererProps) => {
    return ({row}: {row: GridRowType}) => {
        const id = row.id;

        if (id === '-1') return null;

        const checked = Boolean((row as Record<string, unknown>)[columnName]);

        const handleToggle = (next: boolean) => {
            setLocalRows((previousRows) =>
                previousRows.map((currentRow) =>
                    currentRow.id === id ? {...currentRow, [columnName]: next} : currentRow
                )
            );

            updateRowMutation.mutate({
                input: {environmentId, id, tableId, values: {[columnName]: next}},
            });
        };

        return (
            <div className="flex h-full w-full items-center justify-center">
                <Checkbox
                    aria-label={`Toggle ${columnName}`}
                    checked={checked}
                    className="size-4 cursor-pointer"
                    onCheckedChange={(value) => handleToggle(value === true)}
                />
            </div>
        );
    };
};
