/** @type {import('tailwindcss').Config} */

module.exports = {
    content: ['./src/**/*.{js,ts,jsx,tsx}', './*.html'],
    darkMode: 'class',
    important: true,
    plugins: [
        require('@headlessui/tailwindcss'),
        require('tailwindcss-animate'),
        require('@tailwindcss/forms'),
    ],
    theme: {
        container: {
            center: true,
            padding: '2rem',
            screens: {
                '2xl': '1400px',
            },
        },
        extend: {
            animation: {
                'accordion-down': 'accordion-down 0.2s ease-out',
                'accordion-up': 'accordion-up 0.2s ease-out',
            },
            colors: {
                accent: {
                    DEFAULT: 'hsl(var(--accent))',
                    foreground: 'hsl(var(--accent-foreground))',
                },
                background: 'hsl(var(--background))',
                border: 'hsl(var(--border))',
                borderRadius: {
                    lg: 'var(--radius)',
                    md: 'calc(var(--radius) - 2px)',
                    sm: 'calc(var(--radius) - 4px)',
                },
                card: {
                    DEFAULT: 'hsl(var(--card))',
                    foreground: 'hsl(var(--card-foreground))',
                },
                destructive: {
                    DEFAULT: 'hsl(var(--destructive))',
                    foreground: 'hsl(var(--destructive-foreground))',
                },
                foreground: 'hsl(var(--foreground))',
                input: 'hsl(var(--input))',
                muted: {
                    DEFAULT: 'hsl(var(--muted))',
                    foreground: 'hsl(var(--muted-foreground))',
                },
                popover: {
                    DEFAULT: 'hsl(var(--popover))',
                    foreground: 'hsl(var(--popover-foreground))',
                },
                primary: {
                    DEFAULT: 'hsl(var(--primary))',
                    foreground: 'hsl(var(--primary-foreground))',
                },
                ring: 'hsl(var(--ring))',
                secondary: {
                    DEFAULT: 'hsl(var(--secondary))',
                    foreground: 'hsl(var(--secondary-foreground))',
                },
            },
            keyframes: {
                'accordion-down': {
                    from: {height: 0},
                    to: {height: 'var(--radix-accordion-content-height)'},
                },
                'accordion-up': {
                    from: {height: 'var(--radix-accordion-content-height)'},
                    to: {height: 0},
                },
            },
            maxHeight: {
                'select-content-available-height':
                    'var(--radix-select-content-available-height)',
                'select-content-available-height-1/2':
                    'calc(var(--radix-select-content-available-height) / 2)',
            },
            maxWidth: {
                'select-trigger-width': 'var(--radix-select-trigger-width)',
                'tooltip-lg': '500px',
                'tooltip-sm': '250px',
            },
            minWidth: {
                'select-trigger-width': 'var(--radix-select-trigger-width)',
            },
            origin: {
                'select-content-transform-origin':
                    '--radix-select-content-transform-origin',
            },
            width: {
                112: '30rem',
                'select-trigger-width': 'var(--radix-select-trigger-width)',
            },
        },
    },
};
