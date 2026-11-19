export interface WorkflowDefinitionNodeI {
    name: string;
    type: string;
}

interface WorkflowDefinitionI {
    tasks?: unknown;
    triggers?: unknown;
}

const isWorkflowDefinitionNode = (value: unknown): value is WorkflowDefinitionNodeI & {parameters?: unknown} =>
    typeof value === 'object' &&
    value !== null &&
    typeof (value as {name?: unknown}).name === 'string' &&
    typeof (value as {type?: unknown}).type === 'string';

const collectNestedNodes = (value: unknown, collectedNodes: WorkflowDefinitionNodeI[]) => {
    if (Array.isArray(value)) {
        collectNodes(value, collectedNodes);

        return;
    }

    if (typeof value === 'object' && value !== null) {
        for (const nestedValue of Object.values(value)) {
            collectNestedNodes(nestedValue, collectedNodes);
        }
    }
};

const collectNodes = (nodes: unknown, collectedNodes: WorkflowDefinitionNodeI[]) => {
    if (!Array.isArray(nodes)) {
        return;
    }

    for (const node of nodes) {
        if (isWorkflowDefinitionNode(node)) {
            collectedNodes.push({name: node.name, type: node.type});

            collectNestedNodes(node.parameters, collectedNodes);
        } else {
            collectNestedNodes(node, collectedNodes);
        }
    }
};

export function getWorkflowDefinitionNodes(definition: WorkflowDefinitionI): WorkflowDefinitionNodeI[] {
    const collectedNodes: WorkflowDefinitionNodeI[] = [];

    collectNodes(definition.tasks, collectedNodes);
    collectNodes(definition.triggers, collectedNodes);

    return collectedNodes;
}
