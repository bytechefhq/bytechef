import {Checkbox} from '@/components/ui/checkbox';

import useBooleanCellEditor from './hooks/useBooleanCellEditor';

import type {BooleanCellRendererProps, GridRowType} from './types';

export const createBooleanCellRenderer = ({
    columnName,
    onToggle,
    readOnly = false,
    setLocalRows,
}: BooleanCellRendererProps) => {
    return ({row}: {row: GridRowType}) => {
        if (row.id === '-1') {
            return null;
        }

        const {checked, handleToggle} = useBooleanCellEditor({
            columnName,
            onToggle,
            row,
            setLocalRows,
        });

        return (
            <div className="flex h-full w-full items-center justify-center">
                <Checkbox
                    aria-label={`Toggle ${columnName}`}
                    checked={checked}
                    className="size-4 cursor-pointer"
                    disabled={readOnly}
                    onCheckedChange={(value) => handleToggle(value === true)}
                />
            </div>
        );
    };
};
