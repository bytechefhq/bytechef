import {useEffect, useRef} from 'react';

export default function useDebouncedSave<T>(value: T, save: (value: T) => void, delayMs = 600) {
    const lastSavedRef = useRef<string>(JSON.stringify(value));
    const saveRef = useRef(save);

    saveRef.current = save;

    useEffect(() => {
        const serialized = JSON.stringify(value);

        if (serialized === lastSavedRef.current) {
            return;
        }

        const timeout = setTimeout(() => {
            lastSavedRef.current = serialized;

            saveRef.current(value);
        }, delayMs);

        return () => clearTimeout(timeout);
    }, [delayMs, value]);
}
