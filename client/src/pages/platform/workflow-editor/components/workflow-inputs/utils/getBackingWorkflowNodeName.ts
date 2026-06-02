import {Workflow} from '@/shared/middleware/platform/configuration';

/**
 * Finds the name of the first workflow node (task, then trigger) that uses the given component, so a
 * component-referenced input's dynamic options can load through that node's test-configuration
 * connection. Returns undefined when no such node exists; callers then degrade to free-text.
 */
export function getBackingWorkflowNodeName(workflow: Workflow, componentName?: string): string | undefined {
    if (!componentName || !workflow.definition) {
        return undefined;
    }

    let definition: {tasks?: unknown; triggers?: unknown};

    try {
        definition = JSON.parse(workflow.definition);
    } catch {
        return undefined;
    }

    const prefix = `${componentName}/`;

    const findNodeName = (nodes: unknown): string | undefined => {
        if (!Array.isArray(nodes)) {
            return undefined;
        }

        for (const node of nodes) {
            const type = (node as {type?: string})?.type;
            const name = (node as {name?: string})?.name;

            if (type && name && type.startsWith(prefix)) {
                return name;
            }
        }

        return undefined;
    };

    return findNodeName(definition.tasks) ?? findNodeName(definition.triggers);
}
