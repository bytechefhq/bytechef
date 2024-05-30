import {createContext, useContext, useEffect, useState} from 'react';

type ThemeType = 'dark' | 'light' | 'system';

interface ThemeProviderProps {
    children: React.ReactNode;
    defaultTheme?: ThemeType;
    storageKey?: string;
}

interface ThemeProviderStateI {
    theme: ThemeType;
    setTheme: (theme: ThemeType) => void;
}

const initialState: ThemeProviderStateI = {
    setTheme: () => null,
    theme: 'system',
};

const ThemeProviderContext = createContext<ThemeProviderStateI>(initialState);

export function ThemeProvider({
    children,
    defaultTheme = 'system',
    storageKey = 'bytechef-ui-theme',
    ...props
}: ThemeProviderProps) {
    const [theme, setTheme] = useState<ThemeType>(
        () => (localStorage.getItem(storageKey) as ThemeType) || defaultTheme
    );

    useEffect(() => {
        const root = window.document.documentElement;

        root.classList.remove('light', 'dark');

        if (theme === 'system') {
            const systemTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';

            root.classList.add(systemTheme);

            return;
        }

        root.classList.add(theme);
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
