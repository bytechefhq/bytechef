// eslint-disable-next-line @typescript-eslint/no-explicit-any
export default function getParameterByPath(path: string, object: any) {
    return path.split('.').reduce((object, key) => object?.[key], object);
}
