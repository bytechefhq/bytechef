export default function getParameterType(parameterValue: unknown) {
    let parameterType = 'STRING';

    if (Array.isArray(parameterValue)) {
        parameterType = 'ARRAY';
    } else if (typeof parameterValue === 'object') {
        parameterType = 'OBJECT';
    } else if (typeof parameterValue === 'boolean') {
        parameterType = 'BOOLEAN';
    } else if (typeof parameterValue === 'number') {
        parameterType = 'NUMBER';

        if (Number.isInteger(parameterValue)) {
            parameterType = 'INTEGER';
        }
    }

    return parameterType;
}
