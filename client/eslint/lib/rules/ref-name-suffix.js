/* eslint-disable eslint-plugin/prefer-message-ids */
const DESCRIPTION = 'useRef values should be suffixed with `Ref`';

const NAME_PATTERN = /.*Ref/;

module.exports = {
    create(context) {
        return {
            VariableDeclarator(node) {
                if (
                    node.init &&
                    node.init.type === 'CallExpression' &&
                    (node.init.callee.name === 'useRef' ||
                        (node.init.callee.type === 'MemberExpression' &&
                            node.init.callee.object.name === 'React' &&
                            node.init.callee.property.name === 'useRef'))
                ) {
                    const variableName = node.id.name;

                    if (variableName !== 'ref' && !variableName.match(NAME_PATTERN)) {
                        const [variable] = context.getDeclaredVariables(node);
                        const newVariableName = variable.name + 'Ref';

                        for (const reference of variable.references) {
                            context.report({
                                fix: (fixer) => {
                                    return fixer.replaceText(reference.identifier, newVariableName);
                                },
                                message:
                                    reference.identifier === node.id
                                        ? DESCRIPTION
                                        : `ref variable (renamed to ${newVariableName})`,
                                node: reference.identifier,
                            });
                        }
                    }
                }
            },
        };
    },
    meta: {
        docs: {
            category: 'Best Practices',
            description: DESCRIPTION,
        },
        fixable: 'code',
        schema: [],
        type: 'problem',
    },
};
