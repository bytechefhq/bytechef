import {useEffect, useState} from 'react';

const isDarkModeActive = () => document.documentElement.classList.contains('dark');

export default function useIsDarkMode(): boolean {
    const [isDarkMode, setIsDarkMode] = useState(isDarkModeActive);

    useEffect(() => {
        const mutationObserver = new MutationObserver(() => setIsDarkMode(isDarkModeActive()));

        mutationObserver.observe(document.documentElement, {attributeFilter: ['class'], attributes: true});

        setIsDarkMode(isDarkModeActive());

        return () => mutationObserver.disconnect();
    }, []);

    return isDarkMode;
}
