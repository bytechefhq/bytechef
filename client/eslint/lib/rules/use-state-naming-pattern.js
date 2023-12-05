/* eslint-disable eslint-plugin/require-meta-type */
/* eslint-disable eslint-plugin/prefer-message-ids */
const DESCRIPTION = 'useState must follow naming pattern `const [* , set*] =`';

module.exports = {
    create(context) {
        return {
            CallExpression(node) {
                const reactUseState =
                    node.callee.type === 'MemberExpression' &&
                    node.callee.object.type === 'Identifier' &&
                    node.callee.object.name === 'React' &&
                    node.callee.property.type === 'Identifier' &&
                    node.callee.property.name === 'useState';

                const plainUseState = node.callee.type === 'Identifier' && node.callee.name === 'useState';

                if (!reactUseState && !plainUseState) {
                    return;
                }

                const variables = node.parent && node.parent.id && node.parent.id.elements;

                if (!variables || variables.length !== 2) {
                    return;
                }

                const [valueVariable, setterVariable] = variables;

                if (!setterVariable) {
                    return;
                }

                const valueVariableName = valueVariable && valueVariable.name;
                const setterVariableName = setterVariable.name;

                if (!setterVariableName) {
                    return;
                }

                const expectedSetterVariableName = valueVariableName
                    ? `set${valueVariableName.charAt(0).toUpperCase()}${valueVariableName.slice(1)}`
                    : undefined;

                if (!setterVariableName === expectedSetterVariableName) {
                    return;
                }

                const setterStartsWithSet = setterVariableName.slice(0, 3) === 'set';

                if (setterStartsWithSet) {
                    return;
                }

                const variable = context
                    .getDeclaredVariables(node.parent)
                    .find((item) => item.name === setterVariableName);

                for (const reference of variable.references) {
                    context.report({
                        fix: (fixer) => {
                            if (expectedSetterVariableName) {
                                return fixer.replaceText(reference.identifier, expectedSetterVariableName);
                            }
                        },
                        message:
                            reference.identifier === setterVariable
                                ? DESCRIPTION
                                : `${setterVariableName} variable (renamed to ${expectedSetterVariableName})`,
                        node: reference.identifier,
                    });
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
    },
};
