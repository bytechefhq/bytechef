import {
    Content,
    Item,
    Portal,
    Root,
    Trigger,
} from '@radix-ui/react-dropdown-menu';
import {Half2Icon, MoonIcon, SunIcon} from '@radix-ui/react-icons';
import cx from 'classnames';
import React, {useEffect, useState} from 'react';

const availableThemes = [
    {
        key: 'light',
        label: 'Light',
        icon: <SunIcon className="h-5 w-5 text-gray-700 dark:text-gray-300" />,
    },
    {
        key: 'dark',
        label: 'Dark',
        icon: <MoonIcon className="h-5 w-5 text-gray-700 dark:text-gray-300" />,
    },

    {
        key: 'system',
        label: 'System',
        icon: (
            <Half2Icon className="h-5 w-5 text-gray-700 dark:text-gray-300" />
        ),
    },
];

const ThemeSwitcher: React.FC = () => {
    const [currentTheme, setCurrentTheme] = useState(availableThemes[2]);

    useEffect(() => {
        const localTheme = localStorage.getItem('theme');

        const prefersDarkQuery = window.matchMedia(
            '(prefers-color-scheme: dark)'
        );

        const setSystemTheme = () =>
            setCurrentTheme(
                availableThemes.find((theme) => theme.key === 'system')!
            );

        if (localTheme) {
            setCurrentTheme(
                availableThemes.find((theme) => theme.key === localTheme)!
            );
        } else {
            prefersDarkQuery.addEventListener('change', setSystemTheme);
        }

        return prefersDarkQuery.removeEventListener('change', setSystemTheme);
    }, []);

    return (
        <div className="relative inline-block text-left">
            <Root>
                <Trigger
                    className={cx(
                        'inline-flex select-none justify-center rounded-md px-2.5 py-2 text-sm font-medium',
                        'bg-white text-gray-900 hover:bg-gray-50 dark:bg-gray-700 dark:text-gray-100 hover:dark:bg-gray-600',
                        'border border-gray-300 dark:border-transparent',
                        'focus:outline-none focus-visible:ring focus-visible:ring-blue-500 focus-visible:ring-opacity-75'
                    )}
                >
                    {currentTheme.icon}

                    <span className="px-2">{currentTheme.label}</span>
                </Trigger>

                <Portal>
                    <Content
                        align="start"
                        sideOffset={5}
                        className={cx(
                            ' radix-side-top:animate-slide-up radix-side-bottom:animate-slide-down',
                            'w-48 rounded-lg px-1.5 py-1 shadow-md md:w-56',
                            'bg-gray-50 dark:bg-gray-700'
                        )}
                    >
                        {availableThemes.map((theme) => {
                            const {key, label, icon} = theme;

                            return (
                                <Item
                                    key={key}
                                    className={cx(
                                        'flex w-full cursor-default select-none items-center rounded-md px-2 py-2 text-xs outline-none',
                                        'text-gray-500 focus:bg-gray-200 dark:text-gray-400 dark:focus:bg-gray-800'
                                    )}
                                    onClick={() => {
                                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                                        (window as any).__setPreferredTheme(
                                            key
                                        );

                                        setCurrentTheme(theme);
                                    }}
                                >
                                    {icon}

                                    <span className="grow px-2 text-gray-700 dark:text-gray-300">
                                        {label}
                                    </span>
                                </Item>
                            );
                        })}
                    </Content>
                </Portal>
            </Root>
        </div>
    );
};

export default ThemeSwitcher;
