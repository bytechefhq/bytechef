import js from '@eslint/js';
import eslintConfigPrettier from 'eslint-config-prettier';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default tseslint.config(
    {
        ignores: ['node_modules', 'dist', 'src/generated'],
    },
    {
        extends: [js.configs.recommended, ...tseslint.configs.recommended, eslintConfigPrettier],
        files: ['**/*.ts'],
        languageOptions: {
            ecmaVersion: 2022,
            globals: {...globals.node, ...globals.browser},
        },
        rules: {
            '@typescript-eslint/consistent-type-imports': 'error',
            'sort-keys': ['error', 'asc', {caseSensitive: true, natural: true}],
        },
    }
);
