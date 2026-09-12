import {useCallback, useRef, useState} from 'react';

export default function useButtonGroupDropdownAlign() {
    const [alignOffset, setAlignOffset] = useState(0);

    const buttonGroupRef = useRef<HTMLDivElement>(null);
    const dropdownMenuTriggerRef = useRef<HTMLButtonElement>(null);

    const handleOpenChange = useCallback((open: boolean) => {
        if (!open || !buttonGroupRef.current || !dropdownMenuTriggerRef.current) {
            return;
        }

        const buttonGroupRect = buttonGroupRef.current.getBoundingClientRect();
        const dropdownMenuTriggerRect = dropdownMenuTriggerRef.current.getBoundingClientRect();

        setAlignOffset(buttonGroupRect.left - dropdownMenuTriggerRect.left);
    }, []);

    return {alignOffset, buttonGroupRef, dropdownMenuTriggerRef, handleOpenChange};
}
