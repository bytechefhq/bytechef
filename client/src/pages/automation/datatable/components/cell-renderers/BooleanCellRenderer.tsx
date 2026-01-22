import {Checkbox} from '@/components/ui/checkbox';

import useBooleanCellEditor from './hooks/useBooleanCellEditor';

import type {CellRendererProps, GridRowType} from './types';

export const createBooleanCellRenderer = ({
    columnName,
    environmentId,
    setLocalRows,
    tableId,
    updateRowMutation,
}: CellRendererProps) => {
    return ({row}: {row: GridRowType}) => {
        if (row.id === '-1') {
            return null;
        }

        const {checked, handleToggle} = useBooleanCellEditor({
            columnName,
            environmentId,
            row,
            setLocalRows,
            tableId,
            updateRowMutation,
        });

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
