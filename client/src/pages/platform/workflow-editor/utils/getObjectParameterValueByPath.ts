// eslint-disable-next-line @typescript-eslint/no-explicit-any
export default function getObjectParameterValueByPath(path: string, parameters: any) {
    return path.split('.').reduce((parameters, key) => parameters?.[key], parameters);
}
