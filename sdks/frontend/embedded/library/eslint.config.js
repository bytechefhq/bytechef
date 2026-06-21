import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';
import eslintConfigPrettier from 'eslint-config-prettier';
import storybook from 'eslint-plugin-storybook';

export default tseslint.config(
    {ignores: ['dist', '.storybook']},
    {
        extends: [js.configs.recommended, ...tseslint.configs.recommended, eslintConfigPrettier],
        files: ['**/*.{ts,tsx}'],
        languageOptions: {
            ecmaVersion: 2020,
            globals: globals.browser,
        },
        plugins: {
            'react-hooks': reactHooks,
            'react-refresh': reactRefresh,
        },
        rules: {
            ...reactHooks.configs.recommended.rules,
            'react-refresh/only-export-components': ['warn', {allowConstantExport: true}],
        },
    },
    {
        files: ['**/*.stories.@(ts|tsx|js|jsx|mjs|cjs)'],
        extends: [...storybook.configs['flat/recommended']],
    }
);
