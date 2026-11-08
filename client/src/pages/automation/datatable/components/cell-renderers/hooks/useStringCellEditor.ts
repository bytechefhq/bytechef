import {type ChangeEvent, type KeyboardEvent, useRef, useState} from 'react';

import type {GridRowType} from '..';

interface UseStringCellEditorProps {
    columnName: string;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
    row: GridRowType;
}

interface UseStringCellEditorI {
    handleCancel: () => void;
    handleKeyDown: (event: KeyboardEvent<HTMLTextAreaElement>) => void;
    handleOpenChange: (isOpen: boolean) => void;
    handleSave: () => void;
    handleTextChange: (event: ChangeEvent<HTMLTextAreaElement>) => void;
    isOpen: boolean;
    text: string;
}

export default function useStringCellEditor({
    columnName,
    onRowChange,
    row,
}: UseStringCellEditorProps): UseStringCellEditorI {
    const cellValue = (row as Record<string, unknown>)[columnName];

    const [isOpen, setIsOpen] = useState(true);
    const [originalRow] = useState(row);
    const [text, setText] = useState<string>(String((cellValue ?? '') as string));

    const committedRef = useRef(false);

    const handleSave = () => {
        committedRef.current = true;

        onRowChange({...row, [columnName]: text}, true);
        setIsOpen(false);
    };

    const handleCancel = () => {
        if (committedRef.current) {
            return;
        }

        committedRef.current = true;

        onRowChange(originalRow, true);
        setIsOpen(false);
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
        if (event.key === 'Enter' && event.shiftKey) {
            event.stopPropagation();

            return;
        }

        if (event.key === 'Enter') {
            event.preventDefault();
            event.stopPropagation();

            handleSave();

            return;
        }

        if (event.key === 'Escape') {
            event.stopPropagation();

            handleCancel();

            return;
        }
    };

    const handleOpenChange = (openState: boolean) => {
        if (!openState && !committedRef.current) {
            handleSave();

            return;
        }

        setIsOpen(openState);
    };

    const handleTextChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
        setText(event.target.value);

        onRowChange({...row, [columnName]: event.target.value}, false);
    };

    return {
        handleCancel,
        handleKeyDown,
        handleOpenChange,
        handleSave,
        handleTextChange,
        isOpen,
        text,
    };
}
