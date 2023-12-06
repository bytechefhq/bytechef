/* eslint-disable eslint-plugin/prefer-message-ids */
module.exports = {
    create(context) {
        return {
            CallExpression(node) {
                if (
                    node.callee.object &&
                    node.callee.object.name === 'Object' &&
                    node.callee.property &&
                    node.callee.property.name === 'keys'
                ) {
                    const {parent} = node;

                    if (parent.type === 'MemberExpression') {
                        return;
                    }

                    const grandParent = parent && parent.parent;

                    let message;

                    if (parent.type === 'UnaryExpression' && parent.operator === '!') {
                        if (grandParent && grandParent.type === 'UnaryExpression' && grandParent.operator === '!') {
                            message = `!!Object.keys({}) always evaluates to true`;
                        } else {
                            message = `!Object.keys({}) always evaluates to false`;
                        }
                    } else if (parent.type === 'LogicalExpression' || parent.type === 'IfStatement') {
                        message = `Object.keys({}) always evaluates to truthy`;
                    }

                    if (message) {
                        context.report({
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
            description: 'Object.keys({}) always evaluates truthy',
            recommended: false,
        },
        fixable: 'code',
        schema: [],
        type: 'problem',
    },
};
