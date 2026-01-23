import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
import {Plus} from 'lucide-react';
import {type Dispatch, type KeyboardEvent, type SetStateAction} from 'react';

import {type GridRowType} from '../cell-renderers';

interface RowIdCellProps {
    hoveredRowId: string | null;
    onAddRow: () => void;
    onSelectedRowsChange: (rows: ReadonlySet<string>) => void;
    row: GridRowType;
    rowIdx: number;
    selectedRows: ReadonlySet<string>;
    setHoveredRowId: Dispatch<SetStateAction<string | null>>;
}

const RowIdCell = ({
    hoveredRowId,
    onAddRow,
    onSelectedRowsChange,
    row,
    rowIdx,
    selectedRows,
    setHoveredRowId,
}: RowIdCellProps) => {
    const id = row.id;

    // Synthetic last row: show the "+ Add row" button
    if (id === '-1') {
        return (
            <div className="flex h-full w-full items-center justify-center">
                <Button
                    icon={<Plus className="mr-1 h-4 w-4" />}
                    onClick={onAddRow}
                    size="icon"
                    title="Add row"
                    variant="ghost"
                />
            </div>
        );
    }

    const isSelected = selectedRows.has(id);
    const showCheckbox = isSelected || hoveredRowId === id;

    const handleToggleRow = () => {
        const next = new Set<string>(selectedRows);

        if (next.has(id)) {
            next.delete(id);
        } else {
            next.add(id);
        }

        onSelectedRowsChange(next);
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();

            handleToggleRow();
        }
    };

    const handleCheckedChange = (isChecked: boolean | 'indeterminate') => {
        const updatedSelection = new Set<string>(selectedRows);

        if (isChecked === true) {
            updatedSelection.add(id);
        } else {
            updatedSelection.delete(id);
        }

        onSelectedRowsChange(updatedSelection);
    };

    // Use rowIdx directly instead of O(n) findIndex
    const rowNumber = rowIdx + 1;

    return (
        <div
            className="flex w-full items-center justify-center gap-2 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-1"
            onClick={handleToggleRow}
            onKeyDown={handleKeyDown}
            onMouseEnter={() => setHoveredRowId(id)}
            onMouseLeave={() => setHoveredRowId((curr) => (curr === id ? null : curr))}
            role="button"
            tabIndex={0}
            title={`Row ${rowNumber}`}
        >
            {showCheckbox ? (
                <Checkbox
                    aria-label={`Select row ${rowNumber}`}
                    checked={isSelected}
                    className="h-4 w-4 cursor-pointer"
                    onCheckedChange={handleCheckedChange}
                    onClick={(event) => event.stopPropagation()}
                />
            ) : (
                <span aria-hidden="true" className="select-none text-xs text-muted-foreground">
                    {rowNumber}
                </span>
            )}
        </div>
    );
};

export default RowIdCell;
