interface OperationI {
    name: string;
}

interface ResolveMissingDefinitionMessageProps {
    clusterElementType?: string;
    componentDefinition?: {actions?: Array<OperationI>; name: string; triggers?: Array<OperationI>; version: number};
    componentDefinitionLookupFailed: boolean;
    componentName?: string;
    componentVersion?: number;
    operationName?: string;
    taskDispatcher?: boolean;
    trigger?: boolean;
}

export default function resolveMissingDefinitionMessage({
    clusterElementType,
    componentDefinition,
    componentDefinitionLookupFailed,
    componentName,
    componentVersion,
    operationName,
    taskDispatcher,
    trigger,
}: ResolveMissingDefinitionMessageProps): string | undefined {
    if (taskDispatcher || clusterElementType || !componentName) {
        return undefined;
    }

    const version = componentVersion || 1;

    if (!componentDefinition) {
        return componentDefinitionLookupFailed
            ? `Component '${componentName}' (version ${version}) is not available`
            : undefined;
    }

    if (!operationName) {
        return undefined;
    }

    const operations = (trigger ? componentDefinition.triggers : componentDefinition.actions) || [];

    if (operations.some((operation) => operation.name === operationName)) {
        return undefined;
    }

    return `${trigger ? 'Trigger' : 'Action'} '${operationName}' does not exist in component '${componentName}' (version ${version})`;
}
