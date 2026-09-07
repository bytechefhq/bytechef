import {ComponentDefinition} from '@/shared/middleware/platform/configuration';

import {convertNameToSnakeCase} from '../../cluster-element-editor/utils/clusterElementsUtils';

export default function resolveOperationNameForVersion({
    clusterElementType,
    componentDefinition,
    currentOperationName,
    trigger,
}: {
    clusterElementType?: string;
    componentDefinition: ComponentDefinition;
    currentOperationName?: string;
    trigger?: boolean;
}): string | undefined {
    let operations: Array<{name?: string}>;

    if (clusterElementType) {
        const snakeCaseClusterElementType = convertNameToSnakeCase(clusterElementType);

        operations = (componentDefinition.clusterElements ?? []).filter(
            (clusterElement) => clusterElement.type === snakeCaseClusterElementType
        );
    } else if (trigger) {
        operations = componentDefinition.triggers ?? [];
    } else {
        operations = componentDefinition.actions ?? [];
    }

    if (currentOperationName && operations.some((operation) => operation.name === currentOperationName)) {
        return currentOperationName;
    }

    return operations[0]?.name;
}
