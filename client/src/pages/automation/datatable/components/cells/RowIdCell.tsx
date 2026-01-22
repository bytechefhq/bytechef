import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
import {Plus} from 'lucide-react';
import {type Dispatch, type SetStateAction} from 'react';

import {type GridRowType} from '../cell-renderers';

interface RowIdCellProps {
    hoveredRowId: string | null;
    localRows: GridRowType[];
    onAddRow: () => void;
    onSelectedRowsChange: (rows: ReadonlySet<string>) => void;
    row: GridRowType;
    selectedRows: ReadonlySet<string>;
    setHoveredRowId: Dispatch<SetStateAction<string | null>>;
}

const RowIdCell = ({
    hoveredRowId,
    localRows,
    onAddRow,
    onSelectedRowsChange,
    row,
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

    const toggleRow = () => {
        const next = new Set<string>(selectedRows);

        if (next.has(id)) {
            next.delete(id);
        } else {
            next.add(id);
        }

        onSelectedRowsChange(next);
    };

    // compute 1-based row number in current order
    const rowIndex = localRows.findIndex((r) => r.id === id);
    const rowNumber = rowIndex >= 0 ? rowIndex + 1 : undefined;

    return (
        <div
            className="flex w-full items-center justify-center gap-2"
            onClick={toggleRow}
            onMouseEnter={() => setHoveredRowId(id)}
            onMouseLeave={() => setHoveredRowId((curr) => (curr === id ? null : curr))}
            role="button"
            title={rowNumber !== undefined ? `Row ${rowNumber}` : undefined}
        >
            {showCheckbox ? (
                <Checkbox
                    aria-label={rowNumber !== undefined ? `Select row ${rowNumber}` : 'Select row'}
                    checked={isSelected}
                    className="h-4 w-4 cursor-pointer"
                    onCheckedChange={(v) => {
                        const next = new Set<string>(selectedRows);

                        if (v === true) {
                            next.add(id);
                        } else {
                            next.delete(id);
                        }

                        onSelectedRowsChange(next);
                    }}
                    onClick={(e) => e.stopPropagation()}
                />
            ) : (
                <span className="select-none text-xs text-muted-foreground">{rowNumber}</span>
            )}
        </div>
    );
};

export default RowIdCell;
