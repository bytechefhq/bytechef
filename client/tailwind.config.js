/** @type {import('tailwindcss').Config} */

module.exports = {
    content: ['./src/**/*.{js,ts,jsx,tsx}', './*.html'],
    darkMode: ['class', 'class'],
    important: true,
    plugins: [require('@tailwindcss/forms'), require('tailwindcss-animate'), require('@tailwindcss/typography')],
    theme: {
        container: {
            center: 'true',
            padding: '2rem',
        },
        extend: {
            animation: {
                'accordion-down': 'accordion-down 300ms ease-out forwards',
                'accordion-up': 'accordion-up 300ms ease-out forwards',
            },
            backgroundColor: {
                muted: 'hsl(var(--muted))',
            },
            borderRadius: {
                lg: 'var(--radius)',
                md: 'calc(var(--radius) - 2px)',
                sm: 'calc(var(--radius) - 4px)',
                xs: 'calc(var(--radius) - 6px)',
            },
            colors: {
                accent: {
                    DEFAULT: 'hsl(var(--accent))',
                    foreground: 'hsl(var(--accent-foreground))',
                },
                background: 'hsl(var(--background))',
                border: 'hsl(var(--border))',
                button: 'hsl(var(--brand-500))',
                card: {
                    DEFAULT: 'hsl(var(--card))',
                    foreground: 'hsl(var(--card-foreground))',
                },
                chart: {
                    1: 'hsl(var(--chart-1))',
                    2: 'hsl(var(--chart-2))',
                    3: 'hsl(var(--chart-3))',
                    4: 'hsl(var(--chart-4))',
                    5: 'hsl(var(--chart-5))',
                },
                content: {
                    brand: {
                        primary: 'hsl(var(--content-brand-primary))',
                        'primary-hover': 'hsl(var(--content-brand-primary-hover))',
                        'primary-active': 'hsl(var(--content-brand-primary-active))',
                        secondary: 'hsl(var(--content-brand-secondary))',
                    },
                    destructive: {
                        DEFAULT: 'hsl(var(--content-destructive))',
                        primary: 'hsl(var(--content-destructive-primary))',
                        secondary: 'hsl(var(--content-destructive-secondary))',
                    },
                    neutral: {
                        primary: 'hsl(var(--content-neutral-primary))',
                        secondary: 'hsl(var(--content-neutral-secondary))',
                        'secondary-hover': 'hsl(var(--content-neutral-secondary-hover))',
                        tertiary: 'hsl(var(--content-neutral-tertiary))',
                    },
                    onsurface: {
                        primary: 'hsl(var(--content-onsurface-primary))',
                        secondary: 'hsl(var(--content-onsurface-secondary))',
                    },
                    onwarning: 'hsl(var(--content-onwarning))',
                    disabled: 'hsl(var(--content-disabled))',
                    canvas_dot: 'hsl(var(--content-canvas_dot))',
                    success: {
                        DEFAULT: 'hsl(var(--content-success))',
                        primary: 'hsl(var(--content-success-primary))',
                    },
                    warning: {
                        DEFAULT: 'hsl(var(--content-warning))',
                        primary: 'hsl(var(--content-warning-primary))',
                    },
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
                skeleton: 'hsl(var(--skeleton))',
                stroke: {
                    branch: {
                        primary: 'hsl(var(--stroke-branch-primary))',
                        secondary: 'hsl(var(--stroke-branch-secondary))',
                        tertiary: 'hsl(var(--stroke-branch-tertiary))',
                    },
                    brand: {
                        primary: 'hsl(var(--stroke-brand-primary))',
                        'primary-hover': 'hsl(var(--stroke-brand-primary-hover))',
                        'primary-active': 'hsl(var(--stroke-brand-primary-active))',
                        'primary-pressed': 'hsl(var(--stroke-brand-primary-pressed))',
                        secondary: 'hsl(var(--stroke-brand-secondary))',
                        'secondary-hover': 'hsl(var(--stroke-brand-secondary-hover))',
                        'secondary-active': 'hsl(var(--stroke-brand-secondary-active))',
                    },
                    condition: {
                        primary: 'hsl(var(--stroke-condition-primary))',
                        secondary: 'hsl(var(--stroke-condition-secondary))',
                        tertiary: 'hsl(var(--stroke-condition-tertiary))',
                    },
                    destructive: {
                        primary: 'hsl(var(--stroke-destructive-primary))',
                        'primary-hover': 'hsl(var(--stroke-destructive-primary-hover))',
                        'primary-active': 'hsl(var(--stroke-destructive-primary-active))',
                        secondary: 'hsl(var(--stroke-destructive-secondary))',
                        'secondary-hover': 'hsl(var(--stroke-destructive-secondary-hover))',
                    },
                    disabled: 'hsl(var(--stroke-disabled))',
                    each: {
                        primary: 'hsl(var(--stroke-each-primary))',
                        secondary: 'hsl(var(--stroke-each-secondary))',
                        tertiary: 'hsl(var(--stroke-each-tertiary))',
                    },
                    'fork-join': {
                        primary: 'hsl(var(--stroke-fork-join-primary))',
                        secondary: 'hsl(var(--stroke-fork-join-secondary))',
                        tertiary: 'hsl(var(--stroke-fork-join-tertiary))',
                    },
                    loop: {
                        primary: 'hsl(var(--stroke-loop-primary))',
                        secondary: 'hsl(var(--stroke-loop-secondary))',
                        tertiary: 'hsl(var(--stroke-loop-tertiary))',
                        'break-primary': 'hsl(var(--stroke-loop-break-primary))',
                        'break-secondary': 'hsl(var(--stroke-loop-break-secondary))',
                        'break-tertiary': 'hsl(var(--stroke-loop-break-tertiary))',
                    },
                    map: {
                        primary: 'hsl(var(--stroke-map-primary))',
                        secondary: 'hsl(var(--stroke-map-secondary))',
                        tertiary: 'hsl(var(--stroke-map-tertiary))',
                    },
                    neutral: {
                        primary: 'hsl(var(--stroke-neutral-primary))',
                        'primary-hover': 'hsl(var(--stroke-neutral-primary-hover))',
                        secondary: 'hsl(var(--stroke-neutral-secondary))',
                        tertiary: 'hsl(var(--stroke-neutral-tertiary))',
                    },
                    onsurface: {
                        primary: 'hsl(var(--stroke-onsurface-primary))',
                        'primary-hover': 'hsl(var(--stroke-onsurface-primary-hover))',
                    },
                    parallel: {
                        primary: 'hsl(var(--stroke-parallel-primary))',
                        secondary: 'hsl(var(--stroke-parallel-secondary))',
                        tertiary: 'hsl(var(--stroke-parallel-tertiary))',
                    },
                    subflow: {
                        primary: 'hsl(var(--stroke-subflow-primary))',
                        secondary: 'hsl(var(--stroke-subflow-secondary))',
                        tertiary: 'hsl(var(--stroke-subflow-tertiary))',
                    },
                    success: {
                        primary: 'hsl(var(--stroke-success-primary))',
                        'primary-hover': 'hsl(var(--stroke-success-primary-hover))',
                        'primary-active': 'hsl(var(--stroke-success-primary-active))',
                        secondary: 'hsl(var(--stroke-success-secondary))',
                    },
                    warning: {
                        primary: 'hsl(var(--stroke-warning-primary))',
                        'primary-hover': 'hsl(var(--stroke-warning-primary-hover))',
                        'primary-active': 'hsl(var(--stroke-warning-primary-active))',
                    },
                },
                success: {
                    DEFAULT: 'hsl(var(--success))',
                    foreground: 'hsl(var(--success-foreground))',
                },
                surface: {
                    brand: {
                        primary: 'hsl(var(--surface-brand-primary))',
                        'primary-active': 'hsl(var(--surface-brand-primary-active))',
                        'primary-disabled': 'hsl(var(--surface-brand-primary-disabled))',
                        'primary-hover': 'hsl(var(--surface-brand-primary-hover))',
                        secondary: 'hsl(var(--surface-brand-secondary))',
                        'secondary-hover': 'hsl(var(--surface-brand-secondary-hover))',
                        tertiary: 'hsl(var(--surface-brand-tertiary))',
                    },
                    destructive: {
                        primary: 'hsl(var(--surface-destructive-primary))',
                        'primary-active': 'hsl(var(--surface-destructive-primary-active))',
                        'primary-hover': 'hsl(var(--surface-destructive-primary-hover))',
                        secondary: 'hsl(var(--surface-destructive-secondary))',
                        'secondary-hover': 'hsl(var(--surface-destructive-secondary-hover))',
                        'secondary-active': 'hsl(var(--surface-destructive-secondary-active))',
                    },
                    disabled: 'hsl(var(--surface-disabled))',
                    main: 'hsl(var(--surface-main))',
                    neutral: {
                        primary: 'hsl(var(--surface-neutral-primary))',
                        'primary-active': 'hsl(var(--surface-neutral-primary-active))',
                        'primary-hover': 'hsl(var(--surface-neutral-primary-hover))',
                        secondary: 'hsl(var(--surface-neutral-secondary))',
                        'secondary-active': 'hsl(var(--surface-neutral-secondary-active))',
                        'secondary-hover': 'hsl(var(--surface-neutral-secondary-hover))',
                        tertiary: 'hsl(var(--surface-neutral-tertiary))',
                        'tertiary-hover': 'hsl(var(--surface-neutral-tertiary-hover))',
                        transparent: 'hsl(var(--surface-neutral-transparent))',
                    },
                    overlay: {
                        primary: 'hsl(var(--surface-overlay-primary))',
                    },
                    popover: {
                        canvas: 'hsl(var(--surface-popover-canvas))',
                    },
                    success: {
                        primary: 'hsl(var(--surface-success-primary))',
                        'primary-hover': 'hsl(var(--surface-success-primary-hover))',
                        'primary-active': 'hsl(var(--surface-success-primary-active))',
                        secondary: 'hsl(var(--surface-success-secondary))',
                        'secondary-hover': 'hsl(var(--surface-success-secondary-hover))',
                    },
                    tooltip: 'hsl(var(--surface-tooltip))',
                    warning: {
                        primary: 'hsl(var(--surface-warning-primary))',
                        secondary: 'hsl(var(--surface-warning-secondary))',
                        'secondary-hover': 'hsl(var(--surface-warning-secondary-hover))',
                        'secondary-active': 'hsl(var(--surface-warning-secondary-active))',
                    },
                },
            },
            height: {
                'connection-list-item-taglist-height': '38px',
                'footer-height': '34px',
                'header-height': '34px',
                'template-height': '600px',
            },
            inset: {
                'data-pill-panel-placement': '536px',
                'minimap-placement': '400px',
                'node-handle-placement': '36px',
                'task-dispatcher-node-handle-placement': '68px',
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
                'dialog-height': '600px',
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
                'workflow-read-only-project-deployment-workflow-sheet-width': '780px',
                'workflow-sidebar-project-version-history-sheet-width': '600px',
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
                'node-popover-width': 'var(--workflow-nodes-popover-component-menu-width)',
                'sidebar-width': '56px',
                'task-filter-dropdown-menu-width': 'calc(var(--workflow-nodes-popover-component-menu-width) - 50px)',
                'workflow-nodes-popover-actions-menu-width': '400px',
                'workflow-nodes-popover-menu-width': '730px',
                'workflow-outputs-sheet-dialog-width': '440px',
            },
        },
    },
};
