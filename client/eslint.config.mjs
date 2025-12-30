// ESLint Flat Config for ByteChef client (ESLint v9)
// Docs: https://eslint.org/docs/latest/use/configure/configuration-files-new

import js from '@eslint/js';
import globals from 'globals';

// Plugins
import react from 'eslint-plugin-react';
import reactHooks from 'eslint-plugin-react-hooks';
import tailwindcss from 'eslint-plugin-tailwindcss';
import sortDestructureKeys from 'eslint-plugin-sort-destructure-keys';
import bytechef from 'eslint-plugin-bytechef';
import storybook from 'eslint-plugin-storybook';
import pluginLingui from 'eslint-plugin-lingui';

// TypeScript flat configs (provides parser + recommended rules)
import tseslint from 'typescript-eslint';

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

  // Plugin presets intentionally not auto-included; rules are set explicitly below.

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
      tailwindcss,
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

      // Custom ByteChef plugin rules
      'bytechef/empty-line-between-elements': 'error',
      'bytechef/group-imports': 'error',
      'bytechef/no-conditional-object-keys': 'error',
      'bytechef/no-duplicate-imports': 'error',
      'bytechef/no-length-jsx-expression': 'error',
      'bytechef/ref-name-suffix': 'error',
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
      'tailwindcss/classnames-order': 'warn',
      'tailwindcss/no-custom-classname': [
        'warn',
        {
          whitelist: [
            'animate-slide-down',
            'animate-slide-up',
            'animate-slide-up-fade',
            'animate-slide-down-fade',
            'animate-slide-left-fade',
            'animate-slide-right-fade',
            'bg-accent',
            'bg-background',
            'bg-content-brand-primary',
            'bg-destructive-foreground',
            'bg-input',
            'bg-muted',
            'bg-popover',
            'bg-success',
            'bg-surface-brand-primary',
            'bg-surface-brand-primary-hover',
            'bg-surface-brand-primary-active',
            'bg-surface-brand-secondary',
            'bg-surface-brand-secondary-hover',
            'bg-surface-destructive-primary',
            'bg-surface-destructive-primary-hover',
            'bg-surface-destructive-primary-active',
            'bg-surface-destructive-secondary',
            'bg-surface-main',
            'bg-surface-neutral-primary',
            'bg-surface-neutral-primary-hover',
            'bg-surface-neutral-secondary',
            'bg-surface-neutral-secondary-hover',
            'bg-surface-neutral-tertiary',
            'bg-surface-popover-canvas',
            'bg-surface-warning-primary',
            'bg-surface-warning-secondary',
            'border-accent',
            'border-content-neutral-tertiary',
            'border-input',
            'border-l-border/50',
            'border-muted',
            'border-primary',
            'border-stroke-brand-primary',
            'border-stroke-brand-primary-pressed',
            'border-stroke-brand-secondary',
            'border-stroke-brand-secondary-hover',
            'border-stroke-destructive-secondary',
            'border-stroke-neutral-primary',
            'border-stroke-neutral-primary-hover',
            'border-stroke-neutral-secondary',
            'border-stroke-neutral-tertiary',
            'divide-muted',
            'fill-content-neutral-secondary',
            'h-connection-list-item-taglist-height',
            'h-footer-height',
            'h-header-height',
            'h-template-height',
            'heading-tertiary',
            'left-node-handle-placement',
            'left-task-dispatcher-node-handle-placement',
            'left-workflow-node-popover-hover',
            'line-clamp-0',
            'line-clamp-2',
            'max-h-dialog-height',
            'max-h-select-content-available-height',
            'max-h-select-content-available-height-1/2',
            'max-h-workflow-execution-content-height',
            'max-h-workflow-test-configuration-dialog-height',
            'max-w-data-pill-panel-width',
            'max-w-integration-connect-portal-dialog-width',
            'max-w-output-tab-sample-data-dialog-width',
            'max-w-select-trigger-width',
            'max-w-tooltip-lg',
            'max-w-tooltip-sm',
            'max-w-workflow-execution-sheet-width',
            'max-w-workflow-execution-content-width',
            'max-w-workflow-inputs-sheet-width',
            'max-w-workflow-integration-portal-configuration-workflow-sheet-width',
            'max-w-workflow-node-details-panel-width',
            'max-w-workflow-outputs-sheet-width',
            'max-w-workflow-read-only-project-deployment-workflow-sheet-width',
            'max-w-workflow-sidebar-project-version-history-sheet-width',
            'max-w-workflow-test-configuration-dialog-width',
            'min-h-output-tab-sample-data-dialog-height',
            'min-w-api-key-dialog-width',
            'min-w-combo-box-popper-anchor-width',
            'min-w-property-code-editor-sheet-connections-sheet-width',
            'min-w-select-trigger-width',
            'min-w-signing-key-dialog-width',
            'min-w-sub-property-popover-width',
            'min-w-workflow-execution-sheet-width',
            'mx-placeholder-node-position',
            'nodrag',
            'nopan',
            'nowheel',
            'outline-ring',
            'pl-property-input-position',
            'right-data-pill-panel-placement',
            'right-minimap-placement',
            'rounded-xs',
            'size-18',
            'sm:max-w-workflow-inputs-sheet-width',
            'stroke-destructive',
            'text-accent-foreground',
            'text-content-brand-primary',
            'text-content-neutral-primary',
            'text-content-neutral-secondary',
            'text-content-neutral-tertiary',
            'text-destructive',
            'text-content-destructive-primary',
            'text-muted-foreground',
            'text-success',
            'text-success-foreground',
            'text-surface-brand-primary',
            'text-content-warning',
            'w-112',
            'w-appearance-theme-choice-skeleton-large-width',
            'w-appearance-theme-choice-skeleton-small-width',
            'w-node-popover-width',
            'w-sidebar-width',
            'w-workflow-nodes-popover-actions-menu-width',
            'w-workflow-nodes-popover-menu-width',
            'w-workflow-outputs-sheet-dialog-width',
            'w-task-filter-dropdown-menu-width',
          ],
        },
      ],
      'tailwindcss/no-contradicting-classname': 'error',
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
];
