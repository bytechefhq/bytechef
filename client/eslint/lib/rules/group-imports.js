/* eslint-disable eslint-plugin/prefer-message-ids */
const {
    getLeadingComments,
    getRequireStatement,
    getSource,
    hasSideEffects,
    isLocal,
    isTypeImport,
    withScope,
} = require('../common/imports');

const DESCRIPTION = 'imports must be grouped';

module.exports = {
    create(context) {
        const imports = [];

        const {scope, visitors} = withScope();

        function expectBlankLines(node, count = 1) {
            const comments = getLeadingComments(node, context);
            const initial = comments[0] || node;
            const token = context.getSourceCode().getTokenBefore(initial, {
                includeComments: true,
            });

            if (token) {
                const source = context.getSourceCode();

                const start = token.range[1];
                const end = initial.range[0];

                const between = source.text.slice(start, end);

                const newlines = [];

                between.replace(/(?:\r\n|\n)[ \t]*/g, (match) => {
                    newlines.push(match);

                    return match;
                });

                const blankLines = Math.max(newlines.length - 1, 0);

                if (blankLines === count) {
                    return;
                }

                const [first, ...rest] = newlines;

                let fixed;
                let problem;

                if (blankLines < count) {
                    fixed = ['\n' + (first || ''), ...rest].join('');
                    problem = 'expected';
                } else if (blankLines > count) {
                    fixed = newlines.slice(blankLines - count).join('');
                    problem = 'unexpected';
                } else {
                    return;
                }

                const message =
                    `${DESCRIPTION} ` + `(${problem} blank line before: ${JSON.stringify(getSource(node))})`;

                context.report({
                    fix: (fixer) => {
                        return fixer.replaceTextRange([start, end], fixed);
                    },
                    message,
                    node,
                });
            }
        }

        function register(node) {
            if (node) {
                imports.push(node);
            }
        }

        return {
            ...visitors,

            CallExpression(node) {
                if (scope.length) {
                    // Only consider `require` calls at the top level.

                    return;
                }

                register(getRequireStatement(node));
            },

            ImportDeclaration(node) {
                register(node);
            },

            ['Program:exit']() {
                /**
                 * Check each import for 5 possible reasons for
                 * requiring a blank line:
                 *
                 * 1. Import is first in the file.
                 * 2. Import has side effects.
                 * 3. Import is preceded by a non-import.
                 * 4. Import starts a new group.
                 * 5. Import is preceded by a leading, non-header comment.
                 *
                 * For everything else, disallow a blank line.
                 */
                for (let i = 0; i < imports.length; i++) {
                    if (i === 0) {
                        expectBlankLines(imports[0]);
                        continue;
                    }

                    const current = imports[i];

                    const previous = imports[i - 1];

                    if (current === previous) {
                        // Can happen when a `require` is one of several
                        // VariableDeclarators in a VariableDeclaration; eg.
                        //
                        //      const a = require('a'), require('b');
                        //      ^         ^             ^
                        //      |          \             \
                        //      |           ---------------- VariableDeclators
                        //      current
                        //      (VariableDeclaration)
                        //
                        // ie. the `current` import statement ends up being the
                        // same for both `require` calls. We use the separate
                        // one-require-per-statement rule to break up these.

                        continue;
                    }

                    if (hasSideEffects(current)) {
                        expectBlankLines(current);
                        continue;
                    }

                    const token = context.getSourceCode().getTokenBefore(current);

                    const last = context.getSourceCode().getNodeByRangeIndex(token.range[0]);

                    if (last !== previous) {
                        expectBlankLines(current);
                        continue;
                    }

                    const currentSource = getSource(current);
                    const previousSource = getSource(previous);

                    const changed =
                        isLocal(currentSource) ^ isLocal(previousSource) ||
                        isTypeImport(current) ^ isTypeImport(previous);

                    if (changed) {
                        expectBlankLines(current);
                        continue;
                    }
                    const comments = getLeadingComments(current, context);

                    if (comments.length) {
                        expectBlankLines(current);
                        continue;
                    }

                    expectBlankLines(current, 0);
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
