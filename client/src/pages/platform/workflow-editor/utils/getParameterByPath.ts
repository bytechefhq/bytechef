// eslint-disable-next-line @typescript-eslint/no-explicit-any
export default function getParameterByPath(path: string, workflowComponent: any) {
    return path.split('.').reduce((workflowComponent, key) => workflowComponent?.[key], workflowComponent);
}
