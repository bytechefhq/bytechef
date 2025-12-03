/* eslint-disable eslint-plugin/prefer-message-ids */
const message = "Checking for length in JSX can result in rendering a literal '0'";

module.exports = {
    create(context) {
        return {
            LogicalExpression(node) {
                const leftSideLength = node.left.type === 'MemberExpression' && node.left.property.name === 'length';
                const rightSideLength = node.right.type === 'MemberExpression' && node.right.property.name === 'length';

                if (!leftSideLength && rightSideLength && node.parent.type === 'JSXExpressionContainer') {
                    return;
                }

                if (rightSideLength || leftSideLength) {
                    const sourceCode = context.getSourceCode();
                    const ancestors = sourceCode.getAncestors(node) || [];
                    const jsxExpressionScope = ancestors.find((ancestor) => ancestor.type === 'JSXExpressionContainer');

                    if (jsxExpressionScope) {
                        context.report({
                            fix: (fixer) => {
                                return fixer.insertTextBefore(node[leftSideLength ? 'left' : 'right'], '!!');
                            },
                            message,
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
            description: message,
            recommended: false,
        },
        fixable: 'code',
        schema: [],
        type: 'problem',
    },
};
