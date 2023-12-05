/* eslint-disable eslint-plugin/prefer-message-ids */
const message = 'Expected an empty line between sibling elements.';

module.exports = {
    create(context) {
        return {
            JSXElement(node) {
                let previousNodeEndLocation;

                node.children.map((childNode, i) => {
                    if (childNode.type === 'JSXElement' || childNode.type === 'JSXExpressionContainer') {
                        if (previousNodeEndLocation + 1 === childNode.loc.start.line) {
                            context.report({
                                fix: (fixer) => {
                                    let insertBeforeNode = childNode;

                                    const previousNode = node.children[i - 1];

                                    if (previousNode.type === 'Literal' || previousNode.type === 'JSXText') {
                                        insertBeforeNode = previousNode;
                                    }

                                    return fixer.insertTextBefore(insertBeforeNode, '\n');
                                },
                                message: 'Expected an empty line between sibling elements.',
                                node: childNode,
                            });
                        }

                        previousNodeEndLocation = childNode.loc.end.line;
                    }
                });
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
