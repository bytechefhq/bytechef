/* eslint-disable eslint-plugin/prefer-message-ids */
const DESCRIPTION = 'test.step() calls must be awaited';

module.exports = {
    create(context) {
        return {
            CallExpression(node) {
                // Check if this is a test.step() call
                if (
                    node.callee.type === 'MemberExpression' &&
                    node.callee.object.type === 'Identifier' &&
                    node.callee.object.name === 'test' &&
                    node.callee.property.type === 'Identifier' &&
                    node.callee.property.name === 'step'
                ) {
                    // Check if the parent is an AwaitExpression
                    const parent = node.parent;

                    if (parent.type !== 'AwaitExpression') {
                        context.report({
                            fix: (fixer) => {
                                return fixer.insertTextBefore(node, 'await ');
                            },
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
