// Flat Config for developing eslint-plugin-bytechef itself (ESLint v9)
// This lints files inside this package: lib/**/* and tests/**/*

import js from '@eslint/js';
import globals from 'globals';
import eslintPlugin from 'eslint-plugin-eslint-plugin';
import n from 'eslint-plugin-n';

export default [
  // Base ESLint recommended
  js.configs.recommended,

  // ESLint plugin authoring rules (replacement for plugin:eslint-plugin/recommended)
  ...(eslintPlugin?.configs?.['flat/recommended'] ? [eslintPlugin.configs['flat/recommended']] : []),

  // Node.js rules (replacement for plugin:n/recommended)
  ...(n?.configs?.['flat/recommended'] ? [n.configs['flat/recommended']] : []),

  // Library source files (CommonJS + Node globals)
  {
    files: ['lib/**/*.js'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'commonjs',
      globals: {
        ...globals.node,
      },
    },
    rules: {
      // Keep this minimal; plugin behavior is validated via tests
    },
  },

  // Tests: CommonJS + Node + Mocha globals
  {
    files: ['tests/**/*.js'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'commonjs',
      globals: {
        ...globals.node,
        ...globals.mocha,
      },
    },
    rules: {
      // Keep this minimal in tests
    },
  },
];
