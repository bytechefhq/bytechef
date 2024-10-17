import {useEffect} from 'react';

export enum KeyboardKey {
    enter = 'Enter',
    // Add more keys as needed
}

export const useKeyDown = (callback: () => void, keys: KeyboardKey[]) => {
    const onKeyDown = (event: KeyboardEvent) => {
        const wasAnyKeyPressed = keys.some((key) => event.key === key);

        if (wasAnyKeyPressed) {
            event.preventDefault();
            callback();
        }
    };

    useEffect(() => {
        document.addEventListener('keydown', onKeyDown);

        // Clean up event listener on unmount
        return () => {
            document.removeEventListener('keydown', onKeyDown);
        };
    }, [keys, callback]);
};
