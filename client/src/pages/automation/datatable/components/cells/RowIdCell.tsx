import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
import {Plus} from 'lucide-react';
import {type Dispatch, type SetStateAction} from 'react';

import {type GridRowType} from '../cell-renderers';
import useRowIdCell from './hooks/useRowIdCell';

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
    const rowId = row.id;

    const {
        handleCheckboxClick,
        handleCheckedChange,
        handleKeyDown,
        handleMouseEnter,
        handleMouseLeave,
        handleToggleRow,
        isSelected,
    } = useRowIdCell({
        onSelectedRowsChange,
        rowId,
        selectedRows,
        setHoveredRowId,
    });

    // Synthetic last row: show the "+ Add row" button
    if (rowId === '-1') {
        return (
            <div className="flex h-full w-full items-center justify-center">
                <Button
                    icon={<Plus className="mr-1 size-4" />}
                    onClick={onAddRow}
                    size="icon"
                    title="Add row"
                    variant="ghost"
                />
            </div>
        );
    }

    const showCheckbox = isSelected || hoveredRowId === rowId;

    // Use rowIdx directly instead of O(n) findIndex
    const rowNumber = rowIdx + 1;

    return (
        <div
            className="flex w-full items-center justify-center gap-2 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-1"
            onClick={handleToggleRow}
            onKeyDown={handleKeyDown}
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
            role="button"
            tabIndex={0}
            title={`Row ${rowNumber}`}
        >
            {showCheckbox ? (
                <Checkbox
                    aria-label={`Select row ${rowNumber}`}
                    checked={isSelected}
                    className="size-4 cursor-pointer"
                    onCheckedChange={handleCheckedChange}
                    onClick={handleCheckboxClick}
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
