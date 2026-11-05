type OperationDefinitionReferenceType = {
    componentName?: string;
    name?: string;
};

type OperationNodeReferenceType = {
    clusterElementType?: string;
    componentName?: string;
    taskDispatcher?: boolean;
    type?: string;
};

export default function isOperationDefinitionForNode(
    definition: OperationDefinitionReferenceType | undefined,
    node: OperationNodeReferenceType | undefined
): boolean {
    if (!definition || !node) {
        return false;
    }

    const [typeComponentName, , typeOperationName] = (node.type ?? '').split('/');

    const nodeComponentName = node.componentName || typeComponentName;

    if (node.taskDispatcher) {
        return definition.name === nodeComponentName;
    }

    if ((definition.componentName ?? nodeComponentName) !== nodeComponentName) {
        return false;
    }

    if (node.clusterElementType || !typeOperationName) {
        return true;
    }

    return definition.name === typeOperationName;
}
