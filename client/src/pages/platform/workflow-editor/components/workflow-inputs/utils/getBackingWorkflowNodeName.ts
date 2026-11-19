import {Workflow} from '@/shared/middleware/platform/configuration';

import {getWorkflowDefinitionNodes} from './getWorkflowDefinitionNodes';

export function getBackingWorkflowNodeName(
    workflow: Workflow,
    componentName?: string,
    componentVersion?: number
): string | undefined {
    if (!componentName || componentVersion === undefined || !workflow.definition) {
        return undefined;
    }

    let definition: {tasks?: unknown; triggers?: unknown};

    try {
        definition = JSON.parse(workflow.definition);
    } catch {
        return undefined;
    }

    const typePrefix = `${componentName}/v${componentVersion}/`;

    const backingNode = getWorkflowDefinitionNodes(definition).find((node) => node.type.startsWith(typePrefix));

    return backingNode?.name;
}
