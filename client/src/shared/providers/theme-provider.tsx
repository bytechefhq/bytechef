import {ReactNode, createContext, useContext, useEffect, useState} from 'react';

type ThemeType = 'dark' | 'light' | 'system';

interface ThemeProviderProps {
    children: ReactNode;
    defaultTheme?: ThemeType;
    storageKey?: string;
}

interface ThemeProviderStateI {
    theme: ThemeType;
    setTheme: (theme: ThemeType) => void;
}

const ThemeProviderContext = createContext<ThemeProviderStateI | undefined>(undefined);

export function ThemeProvider({
    children,
    defaultTheme = 'light',
    storageKey = 'bytechef.ui-theme',
    ...props
}: ThemeProviderProps) {
    const [theme, setTheme] = useState<ThemeType>(
        () => (localStorage.getItem(storageKey) as ThemeType) || defaultTheme
    );

    useEffect(() => {
        const root = window.document.documentElement;
        const darkModeQuery = window.matchMedia('(prefers-color-scheme: dark)');

        const applyTheme = () => {
            root.classList.remove('light', 'dark');

            if (theme === 'system') {
                root.classList.add(darkModeQuery.matches ? 'dark' : 'light');
            } else {
                root.classList.add(theme);
            }
        };

        applyTheme();

        if (theme !== 'system') {
            return;
        }

        darkModeQuery.addEventListener('change', applyTheme);

        return () => darkModeQuery.removeEventListener('change', applyTheme);
    }, [theme]);

    const value = {
        setTheme: (theme: ThemeType) => {
            localStorage.setItem(storageKey, theme);
            setTheme(theme);
        },
        theme,
    };

    return (
        <ThemeProviderContext.Provider {...props} value={value}>
            {children}
        </ThemeProviderContext.Provider>
    );
}

export const useTheme = () => {
    const context = useContext(ThemeProviderContext);

    if (context === undefined) throw new Error('useTheme must be used within a ThemeProvider');

    return context;
};
