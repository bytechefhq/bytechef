/** @type {import('tailwindcss').Config} */

module.exports = {
    content: ['./src/**/*.{js,ts,jsx,tsx}', './*.html'],
    darkMode: 'class',
    important: true,
    plugins: [require('@tailwindcss/forms'), require('tailwindcss-animate')],
    theme: {
        container: {
            center: true,
            padding: '2rem',
        },
        extend: {
            animation: {
                'accordion-down': 'accordion-down 0.2s ease-out',
                'accordion-up': 'accordion-up 0.2s ease-out',
            },
            backgroundColor: {
                muted: 'hsl(var(--muted))',
            },
            borderRadius: {
                lg: 'var(--radius)',
                md: 'calc(var(--radius) - 2px)',
                sm: 'calc(var(--radius) - 4px)',
            },
            colors: {
                accent: {
                    DEFAULT: 'hsl(var(--accent))',
                    foreground: 'hsl(var(--accent-foreground))',
                },
                background: 'hsl(var(--background))',
                border: 'hsl(var(--border))',
                card: {
                    DEFAULT: 'hsl(var(--card))',
                    foreground: 'hsl(var(--card-foreground))',
                },
                destructive: {
                    DEFAULT: 'hsl(var(--destructive))',
                    foreground: 'hsl(var(--destructive-foreground))',
                },
                foreground: 'hsl(var(--foreground))',
                info: {
                    DEFAULT: 'hsl(var(--info))',
                    foreground: 'hsl(var(--info-foreground))',
                },
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
                success: {
                    DEFAULT: 'hsl(var(--success))',
                    foreground: 'hsl(var(--success-foreground))',
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
                'select-content-available-height': 'var(--radix-select-content-available-height)',
                'select-content-available-height-1/2': 'calc(var(--radix-select-content-available-height) / 2)',
            },
            maxWidth: {
                'select-trigger-width': 'var(--radix-select-trigger-width)',
            },
            minWidth: {
                'combo-box-popper-anchor-width': 'var(--radix-popper-anchor-width)',
                'select-trigger-width': 'var(--radix-select-trigger-width)',
            },
            screens: {
                '3xl': '1792px',
            },
            width: {
                'workflow-nodes-popover-menu-width': '500px',
            },
        },
    },
};
