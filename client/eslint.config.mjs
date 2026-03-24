// ESLint Flat Config for ByteChef client (ESLint v9)
// Docs: https://eslint.org/docs/latest/use/configure/configuration-files-new

import js from '@eslint/js';
import globals from 'globals';

// Plugins
import react from 'eslint-plugin-react';
import reactHooks from 'eslint-plugin-react-hooks';
import betterTailwindcss from 'eslint-plugin-better-tailwindcss';
import sortDestructureKeys from 'eslint-plugin-sort-destructure-keys';
import bytechef from 'eslint-plugin-bytechef';
import storybook from 'eslint-plugin-storybook';
import pluginLingui from 'eslint-plugin-lingui';

// TypeScript flat configs (provides parser + recommended rules)
import tseslint from 'typescript-eslint';

import {restrictedImports} from './eslint-restricted-imports.mjs';

// Note: We avoid auto-including plugin "recommended" presets to prevent mixing legacy and flat configs.

export default [
    // Ignores (migrated from .eslintignore)
    {
        ignores: [
            'node_modules',
            'dist',
            'src/components/assistant-ui',
            'src/components/ui',
            'src/hooks/use-toast.ts',
            'src/**/graphql.ts',
            'src/locales/**',
            'src/**/middleware/**',
        ],
    },
    // Base JS recommended rules
    js.configs.recommended,
    // TypeScript recommended rules and parser setup
    ...tseslint.configs.recommended,
    // Lingui plugin recommended rules
    pluginLingui.configs['flat/recommended'],
    // Project-specific rules and plugin configuration
    {
        files: ['**/*.{js,jsx,ts,tsx}'],
        languageOptions: {
            ecmaVersion: 'latest',
            sourceType: 'module',
            parser: tseslint.parser,
            globals: {
                ...globals.browser,
                ...globals.node,
                ...globals.jest,
            },
        },
        settings: {
            react: {version: 'detect'},
        },
        plugins: {
            '@typescript-eslint': tseslint.plugin,
            react,
            'react-hooks': reactHooks,
            'better-tailwindcss': betterTailwindcss,
            'sort-destructure-keys': sortDestructureKeys,
            bytechef,
            storybook,
        },
        rules: {
            // TypeScript
            '@typescript-eslint/explicit-module-boundary-types': 'off',
            '@typescript-eslint/no-non-null-assertion': 'off',
            '@typescript-eslint/naming-convention': [
                'error',
                {
                    selector: 'typeAlias',
                    format: ['PascalCase'],
                    suffix: ['Type'],
                },
                {
                    selector: 'interface',
                    format: ['PascalCase'],
                    suffix: ['Props', 'I'],
                },
            ],
            '@typescript-eslint/no-restricted-imports': ['warn', restrictedImports],

            // Custom ByteChef plugin rules
            'bytechef/empty-line-between-elements': 'error',
            'bytechef/group-imports': 'error',
            'bytechef/no-conditional-object-keys': 'error',
            'bytechef/no-duplicate-imports': 'error',
            'bytechef/no-length-jsx-expression': 'error',
            'bytechef/ref-name-suffix': 'error',
            'bytechef/require-await-test-step': 'error',
            'bytechef/sort-import-destructures': 'error',
            'bytechef/sort-imports': 'error',
            'bytechef/use-state-naming-pattern': 'error',

            // React
            'react/jsx-sort-props': 'error',
            'react/prop-types': 'off',
            'react/react-in-jsx-scope': 'off',

            // React Hooks
            'react-hooks/rules-of-hooks': 'error',
            'react-hooks/exhaustive-deps': 'warn',

            // Sorting
            'sort-destructure-keys/sort-destructure-keys': 2,
            'sort-keys': ['error', 'asc', {caseSensitive: true, natural: true}],

            // Tailwind CSS
            'better-tailwindcss/enforce-consistent-class-order': 'warn',
            'better-tailwindcss/no-unknown-classes': [
                'warn',
                {
                    ignore: [
                        // Animations
                        '^animate-slide-',
                        // Theme colors: bg-*, border-*, text-*, fill-*, stroke-*, divide-*, outline-*
                        '^bg-accent$',
                        '^bg-background$',
                        '^bg-content-',
                        '^bg-destructive-foreground$',
                        '^bg-input$',
                        '^bg-muted$',
                        '^bg-popover$',
                        '^bg-success$',
                        '^bg-surface-',
                        '^border-accent$',
                        '^border-content-',
                        '^border-input$',
                        '^border-l-border/',
                        '^border-muted$',
                        '^border-primary$',
                        '^border-stroke-',
                        '^divide-muted$',
                        '^fill-content-',
                        '^outline-ring$',
                        '^stroke-destructive$',
                        '^text-accent-foreground$',
                        '^text-content-',
                        '^text-destructive$',
                        '^text-muted-foreground$',
                        '^text-normal$',
                        '^text-success',
                        '^text-surface-',
                        // Custom spacing/sizing utilities
                        '^h-connection-list-item-taglist-height$',
                        '^h-footer-height$',
                        '^h-header-height$',
                        '^h-template-height$',
                        '^heading-tertiary$',
                        '^left-node-handle-placement$',
                        '^left-task-dispatcher-node-handle-placement$',
                        '^left-workflow-node-popover-hover$',
                        '^line-clamp-',
                        '^max-h-',
                        '^max-w-',
                        '^min-h-',
                        '^min-w-',
                        '^mx-placeholder-node-position$',
                        '^pl-property-input-position$',
                        '^right-data-pill-panel-placement$',
                        '^right-minimap-placement$',
                        '^rounded-xs$',
                        '^size-18$',
                        '^sm:max-w-',
                        '^w-112$',
                        '^w-appearance-',
                        '^w-node-popover-width$',
                        '^w-sidebar-width$',
                        '^w-workflow-',
                        '^w-task-filter-dropdown-menu-width$',
                        // ReactFlow classes
                        '^nodrag$',
                        '^nopan$',
                        '^nowheel$',
                        // @assistant-ui/react classes
                        '^aui-',
                        // Container queries
                        '^@container$',
                        '^@md:',
                        // Data attribute variants
                        '^data-',
                        // Radix UI variants
                        '^radix-',
                        // Component-specific classes
                        '^ai-provider$',
                        '^connect-',
                        '^dropdown-menu-item',
                        '^is-(active|checked|selected|unchecked)$',
                        '^not-prose$',
                        '^property-',
                        '^radio-card-',
                        // Arbitrary value selectors
                        '^\\[&',
                        // Hover variants with custom theme
                        '^hover:border-gray-',
                        '^active:text-content-',
                        // Partial classes from dynamic class construction
                        '^px\\]?$',
                        '^left-$',
                    ],
                },
            ],
            'better-tailwindcss/no-conflicting-classes': 'error',
        },
    },

    // Vitest globals for test files
    {
        files: ['**/*.{test,spec}.{js,jsx,ts,tsx}'],
        languageOptions: {
            globals: {
                ...globals.vitest,
            },
        },
    },

    // Allow custom component wrapper imports
    {
        files: [
            'src/components/Badge/Badge.tsx',
            'src/components/Button/Button.tsx',
            'src/components/Switch/Switch.tsx',
        ],
        rules: {
            '@typescript-eslint/no-restricted-imports': 'off',
        },
    },

    // Exclude `ee/` folder from no-restricted-imports rule
    {
        files: ['src/ee/**/*.{js,jsx,ts,tsx}'],
        rules: {
            '@typescript-eslint/no-restricted-imports': 'off',
        },
    },
];
