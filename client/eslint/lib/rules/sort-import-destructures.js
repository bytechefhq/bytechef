/* eslint-disable eslint-plugin/prefer-message-ids */
const DESCRIPTION = 'destructured names in imports must be sorted';

module.exports = {
    create(context) {
        return {
            ImportDeclaration(node) {
                const specifiers = node.specifiers.filter((specifier) => {
                    // Just `ImportSpecifier` (ignore `ImportDefaultSpecifier`).

                    return specifier.type === 'ImportSpecifier';
                });

                if (specifiers.length > 1) {
                    const source = context.getSourceCode();

                    if (source.commentsExistBetween(source.getTokenBefore(specifiers[0]), node.source)) {
                        // Don't touch if any of the specifiers have
                        // comments.

                        return;
                    }

                    // Given:
                    //
                    //      import {a as b, c} from 'd';
                    //
                    // We'll have two specifiers:
                    //
                    // - `imported.name === 'a'`, `local.name === 'b').
                    // - `imported.name === 'c'`.
                    //
                    // We sort by `imported` always, ignoring `local`.

                    const sorted = specifiers.slice().sort((a, b) => {
                        return a.imported.name > b.imported.name ? 1 : -1;
                    });

                    const fix = sorted.some((sorted, i) => sorted !== specifiers[i]);

                    if (fix) {
                        const text = ' '.repeat(node.range[0]) + source.getText(node);

                        const start = specifiers[0].range[0];
                        const end = specifiers[specifiers.length - 1].range[1];

                        let fixed = '';

                        for (let i = 0; i < specifiers.length; i++) {
                            fixed += source.getText(sorted[i]);

                            if (i < specifiers.length - 1) {
                                // Grab all text between specifier and next.

                                const between = text.slice(specifiers[i].range[1], specifiers[i + 1].range[0]);

                                fixed += between;
                            }
                        }

                        context.report({
                            fix: (fixer) => fixer.replaceTextRange([start, end], fixed),
                            message: DESCRIPTION,
                            node,
                        });
                    }
                }
            },
        };
    },

    meta: {
        docs: {
            category: 'Best Practices',
            description: DESCRIPTION,
            recommended: false,
        },
        fixable: 'code',
        schema: [],
        type: 'problem',
    },
};
