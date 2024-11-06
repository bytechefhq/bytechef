/** @type {import('tailwindcss').Config} */

module.exports = {
    content: ['./src/**/*.{js,ts,jsx,tsx}', './*.html'],
    darkMode: 'class',
    important: true,
    plugins: [require('@tailwindcss/forms'), require('tailwindcss-animate')],
    theme: {
        container: {
            center: 'true',
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
                chart: {
                    1: 'hsl(var(--chart-1))',
                    2: 'hsl(var(--chart-2))',
                    3: 'hsl(var(--chart-3))',
                    4: 'hsl(var(--chart-4))',
                    5: 'hsl(var(--chart-5))',
                },
            },
            height: {
                'connection-list-item-taglist-height': '38px',
                'footer-height': '34px',
                'header-height': '34px',
            },
            inset: {
                'data-pill-panel-placement': '485px',
                'node-handle-placement': '36px',
                'workflow-node-popover-hover': '-40px',
            },
            keyframes: {
                'accordion-down': {
                    from: {
                        height: '0',
                    },
                    to: {
                        height: 'var(--radix-accordion-content-height)',
                    },
                },
                'accordion-up': {
                    from: {
                        height: 'var(--radix-accordion-content-height)',
                    },
                    to: {
                        height: '0',
                    },
                },
            },
            margin: {
                'placeholder-node-position': '22px',
            },
            maxHeight: {
                'project-instance-dialog-height': '600px',
                'select-content-available-height': 'var(--radix-select-content-available-height)',
                'select-content-available-height-1/2': 'calc(var(--radix-select-content-available-height) / 2)',
                'workflow-execution-content-height': '80vh',
                'workflow-test-configuration-dialog-height': '700px',
            },
            maxWidth: {
                'data-pill-panel-width': '400px',
                'integration-connect-portal-dialog-width': '600px',
                'output-tab-sample-data-dialog-width': '800px',
                'select-trigger-width': 'var(--radix-select-trigger-width)',
                'workflow-execution-content-width': '1000px',
                'workflow-execution-sheet-width': '500px',
                'workflow-inputs-sheet-width': '700px',
                'workflow-integration-portal-configuration-workflow-sheet-width': '460px',
                'workflow-node-details-panel-width': '460px',
                'workflow-outputs-sheet-width': '700px',
                'workflow-read-only-project-instance-workflow-sheet-width': '780px',
                'workflow-sidebar-project-version-history-sheet-width': '500px',
                'workflow-test-configuration-dialog-width': '600px',
            },
            minHeight: {
                'output-tab-sample-data-dialog-height': '400px',
            },
            minWidth: {
                'api-key-dialog-width': '550px',
                'combo-box-popper-anchor-width': 'var(--radix-popper-anchor-width)',
                'property-code-editor-sheet-connections-sheet-width': '400px',
                'select-trigger-width': 'var(--radix-select-trigger-width)',
                'signing-key-dialog-width': '550px',
                'sub-property-popover-width': '400px',
                'workflow-execution-sheet-width': '500px',
            },
            padding: {
                'property-input-position': '50px',
            },
            screens: {
                '3xl': '1792px',
            },
            width: {
                'appearance-theme-choice-skeleton-large-width': '100px',
                'appearance-theme-choice-skeleton-small-width': '80px',
                'sidebar-width': '56px',
                'workflow-nodes-popover-actions-menu-width': '400px',
                'workflow-nodes-popover-component-menu-width': '330px',
                'workflow-nodes-popover-menu-width': '730px',
                'workflow-outputs-sheet-dialog-width': '440px',
            },
        },
    },
};
