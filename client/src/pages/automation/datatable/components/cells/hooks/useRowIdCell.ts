import type {Dispatch, KeyboardEvent, MouseEvent, SetStateAction} from 'react';

interface UseRowSelectionCellProps {
    onSelectedRowsChange: (rows: ReadonlySet<string>) => void;
    rowId: string;
    selectedRows: ReadonlySet<string>;
    setHoveredRowId: Dispatch<SetStateAction<string | null>>;
}

interface UseRowSelectionCellI {
    handleCheckboxClick: (event: MouseEvent) => void;
    handleCheckedChange: (isChecked: boolean | 'indeterminate') => void;
    handleKeyDown: (event: KeyboardEvent<HTMLDivElement>) => void;
    handleMouseEnter: () => void;
    handleMouseLeave: () => void;
    handleToggleRow: () => void;
    isSelected: boolean;
}

export default function useRowIdCell({
    onSelectedRowsChange,
    rowId,
    selectedRows,
    setHoveredRowId,
}: UseRowSelectionCellProps): UseRowSelectionCellI {
    const isSelected = selectedRows.has(rowId);

    const handleMouseEnter = () => {
        setHoveredRowId(rowId);
    };

    const handleMouseLeave = () => {
        setHoveredRowId((currentId) => (currentId === rowId ? null : currentId));
    };

    const handleToggleRow = () => {
        const nextSelection = new Set<string>(selectedRows);

        if (nextSelection.has(rowId)) {
            nextSelection.delete(rowId);
        } else {
            nextSelection.add(rowId);
        }

        onSelectedRowsChange(nextSelection);
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
            updatedSelection.add(rowId);
        } else {
            updatedSelection.delete(rowId);
        }

        onSelectedRowsChange(updatedSelection);
    };

    const handleCheckboxClick = (event: MouseEvent) => {
        event.stopPropagation();
    };

    return {
        handleCheckboxClick,
        handleCheckedChange,
        handleKeyDown,
        handleMouseEnter,
        handleMouseLeave,
        handleToggleRow,
        isSelected,
    };
}
