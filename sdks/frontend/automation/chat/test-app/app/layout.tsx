import type {Metadata} from 'next';
import '@bytechef/automation-chat/dist/style.css';
import './globals.css';
import {ThemeSwitch} from './theme-switch';

export const metadata: Metadata = {
    title: 'ByteChef Chat SDK - Test App',
    description: 'Test application for ByteChef Automation Chat SDK',
};

export default function RootLayout({children}: {children: React.ReactNode}) {
    return (
        <html lang="en" suppressHydrationWarning>
            <head>
                <script
                    dangerouslySetInnerHTML={{
                        __html: `
              (function() {
                const savedTheme = localStorage.getItem('theme');
                const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
                const theme = savedTheme || (prefersDark ? 'dark' : 'light');
                if (theme === 'dark') {
                  document.documentElement.classList.add('dark');
                } else {
                  document.documentElement.classList.add('light');
                }
              })();
            `,
                    }}
                />
            </head>
            <body className="antialiased">
                <ThemeSwitch />
                {children}
            </body>
        </html>
    );
}
