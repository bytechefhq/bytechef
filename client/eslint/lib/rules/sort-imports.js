/* eslint-disable eslint-plugin/prefer-message-ids */
const {
    getLeadingComments,
    getRequireStatement,
    getSource,
    getTrailingComments,
    hasSideEffects,
    isAbsolute,
    isRelative,
    isTypeImport,
    withScope,
} = require('../common/imports');

const DESCRIPTION = 'imports must be sorted';

/**
 * Given two sort keys `a` and `b`, return -1, 0 or 1 to indicate
 * their relative ordering.
 */
function compare(aKey, bKey) {
    const [aPrefix, aName, aTieBreaker] = aKey.split(':');
    const [bPrefix, bName, bTieBreaker] = bKey.split(':');

    const cmp = (a, b) => {
        return a < b ? -1 : a > b ? 1 : 0;
    };

    //

    return (
        cmp(aPrefix, bPrefix) ||
        cmp(ranking(aName), ranking(bName)) ||
        cmp(aName, bName) ||
        cmp(aTieBreaker, bTieBreaker)
    );
}

/**
 * Ideally we'd just sort by source, but we need to handle two "edge" cases:
 *
 * - When we have type-only imports, those always come last.
 * - When we import from the same module twice (not always an error; can
 *   legitimately happen if we have both a type-only and a value-only import
 *   from the same module).
 *
 * So, the sort key must include information about both the import kind
 * (this serves as a prefix in the sort key) and any import specifiers
 * that may be present (these serve to construct a trailing "tiebreaker"
 * suffix in the sort key).
 */
function getSortKey(node) {
    const source = getSource(node);
    let tieBreaker;

    // Type-only imports always go last.

    const prefix = isTypeImport(node) ? 'type-import' : 'normal-import';

    if (node.type === 'ImportDeclaration') {
        const specifiers = node.specifiers.map((specifier) => {
            // Note tie breaking order here:
            //
            //      * as name
            //      name
            //      {name}
            //      {name:alias}
            //

            if (specifier.type === 'ImportNamespaceSpecifier') {
                return `*${specifier.local.name}`;
            } else if (specifier.type === 'ImportDefaultSpecifier') {
                return specifier.local.name;
            } else {
                return `{${specifier.imported.name}:${specifier.local.name}}`;
            }
        });

        tieBreaker = specifiers.sort().join(':');
    } else if (node.type === 'VariableDeclaration') {
        // ie. `const ... = require('...');`

        const declarations = node.declarations.map((declaration) => {
            if (declaration.id.type === 'Identifier') {
                return declaration.id.name;
            } else if (declaration.id.type === 'ObjectPattern') {
                const properties = declaration.id.properties.map((property) => {
                    if (property.type === 'Property') {
                        return `${property.key.name}:${property.value.name}`;
                    } else if (property.type === 'ExperimentalRestProperty') {
                        return `...${property.argument.name}`;
                    }
                });

                return `{${properties.sort().join()}}`;
            }
        });

        tieBreaker = declarations.sort().join(':');
    } else if (node.type === 'ExpressionStatement') {
        // ie. `require('...');`
        // Always alone in group so tieBreaker not needed.

        tieBreaker = '';
    }

    return `${prefix}:${source}:${tieBreaker}`;
}

/**
 * Returns a ranking for `source`. Lower-numbered ranks are considered more
 * important and will be sorted first in the file.
 *
 * - 0: NodeJS built-ins and dependencies declared in "package.json" files.
 * - 1: Absolute paths.
 * - 2: Relative paths.
 */
function ranking(source) {
    return isRelative(source) ? 2 : isAbsolute(source) ? 1 : 0;
}

module.exports = {
    create(context) {
        /**
         * A buffer for collecting imports into a group.
         */
        let group = [];

        /**
         * An array of groups of imports. Any import that is made just
         * for its side-effects will start (and end) a new group, and
         * forms a boundary across which we, generally, must not re-order.
         *
         * Type-only imports are an exception because they are independent of
         * evaluation order. That is, they can be safely moved to the end, even
         * moving across "boundaries" created by side-effectful imports.
         */
        const imports = [group];

        const {scope, visitors} = withScope();

        function getRangeForNode(node) {
            const commentsBefore = getLeadingComments(node, context);
            const commentsAfter = getTrailingComments(node, context);

            const first = commentsBefore[0] || node;

            const last = commentsAfter[commentsAfter.length - 1] || node;

            return [first.range[0], last.range[1]];
        }

        function register(node) {
            if (node) {
                if (hasSideEffects(node)) {
                    // Create a boundary group across which we cannot reorder.

                    group = [];
                    imports.push([node], group);
                } else {
                    group.push(node);
                }
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
                // Get global list of all imports across groups in source-order.

                const actual = imports.reduce((accumulator, group) => {
                    accumulator.push(...group);

                    return accumulator;
                }, []);

                // Compute desired global ordering across all groups.

                const typeImports = [];

                const desired = imports
                    .reduce((accumulator, group) => {
                        const sorted = group
                            .filter((node) => {
                                if (isTypeImport(node)) {
                                    typeImports.push(node);

                                    return false;
                                } else {
                                    return true;
                                }
                            })
                            .sort((a, b) => compare(getSortKey(a), getSortKey(b)));

                        return accumulator.concat(sorted);
                    }, [])
                    .concat(typeImports.sort((a, b) => compare(getSortKey(a), getSortKey(b))));

                // Try to make error messages (somewhat) minimal by only
                // reporting from the first to the last mismatch (ie.
                // not a full Myers diff algorithm).

                let firstMismatch = -1;
                let lastMismatch = -1;

                for (let i = 0; i < actual.length; i++) {
                    if (actual[i] !== desired[i]) {
                        firstMismatch = i;
                        break;
                    }
                }

                for (let i = actual.length - 1; i >= 0; i--) {
                    if (actual[i] !== desired[i]) {
                        lastMismatch = i;
                        break;
                    }
                }

                if (firstMismatch === -1) {
                    return;
                }

                const description = desired
                    .slice(firstMismatch, lastMismatch + 1)
                    .map((node) => {
                        const source = JSON.stringify(getSource(node));

                        if (isTypeImport(node)) {
                            return `(type) ${source}`;
                        } else {
                            return source;
                        }
                    })
                    .join(' << ');

                const message = 'imports must be sorted by module name ' + `(expected: ${description})`;

                context.report({
                    fix: (fixer) => {
                        const fixings = [];

                        const code = context.getSourceCode();

                        const sources = new Map();

                        // Pass 1: Extract copy of text.

                        for (let i = firstMismatch; i <= lastMismatch; i++) {
                            const node = actual[i];
                            const range = getRangeForNode(node);
                            const text = code.getText().slice(...range);

                            sources.set(actual[i], {text});
                        }

                        // Pass 2: Write text into expected positions.

                        for (let i = firstMismatch; i <= lastMismatch; i++) {
                            fixings.push(
                                fixer.replaceTextRange(getRangeForNode(actual[i]), sources.get(desired[i]).text)
                            );
                        }

                        return fixings;
                    },
                    message,
                    node: actual[firstMismatch],
                });
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
