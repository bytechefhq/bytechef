// eslint-disable-next-line @typescript-eslint/no-explicit-any
export default function getArrayParameterValueByPath(path: string, parameters: any) {
    return path?.split('[').reduce((parameters, key) => {
        const index = parseInt(key.replace(']', ''));

        if (Array.isArray(parameters)) {
            return parameters[index];
        }

        return parameters?.[key];
    }, parameters);
}
