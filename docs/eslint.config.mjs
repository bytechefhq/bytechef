import js from '@eslint/js';
import next from '@next/eslint-plugin-next';
import tseslint from 'typescript-eslint';

export default [
  {
    ignores: [
      'dist',
      'node_modules',
      '.next/',
      '.source/',
      'out/',
      'next.config.mjs',
      'postcss.config.js',
      // Emitted by `npm run generate:openapi`; fix the generator, not the output.
      'content/docs/openapi/(generated)/',
    ],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  next.configs['core-web-vitals'],
  {
    rules: {
      'no-console': 'off',
      // for Fumadocs CLI
      'import/no-relative-packages': 'off',
      // Several imports are parked next to commented-out usage (the AI search widgets, the Orama
      // index updater). Report them without failing the build.
      '@typescript-eslint/no-unused-vars': [
        'warn',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
    },
  },
];
