/**
 * @fileoverview Bytechef ESLint plugin
 * @author Ivica Cardic
 */
'use strict';

//------------------------------------------------------------------------------
// Requirements
//------------------------------------------------------------------------------

const requireIndex = require('requireindex');

//------------------------------------------------------------------------------
// Plugin Definition
//------------------------------------------------------------------------------

// import all rules in lib/rules
const rules = requireIndex(__dirname + '/rules');

module.exports.rules = rules;

// Presets for consumers (legacy and flat)
module.exports.configs = {
    // Legacy .eslintrc users
    recommended: {
        plugins: ['bytechef'],
        rules: {
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
        },
    },

    // ESLint 9 Flat Config users
    'flat/recommended': [
        {
            plugins: {bytechef: module.exports},
            rules: {
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
            },
        },
    ],
};
