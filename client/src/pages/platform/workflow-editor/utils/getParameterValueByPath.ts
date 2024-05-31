// eslint-disable-next-line @typescript-eslint/no-explicit-any
export default function getParameterValueByPath(path: string, workflowComponent: any) {
    return path.split('.').reduce((workflowComponent, key) => workflowComponent?.[key], workflowComponent);
}
