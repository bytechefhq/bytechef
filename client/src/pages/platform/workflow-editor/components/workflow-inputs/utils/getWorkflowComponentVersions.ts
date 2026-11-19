import {Workflow} from '@/shared/middleware/platform/configuration';

export function getWorkflowComponentVersions(workflow: Workflow): Record<string, number> {
    if (!workflow.definition) {
        return {};
    }

    let definition: {tasks?: unknown; triggers?: unknown};

    try {
        definition = JSON.parse(workflow.definition);
    } catch (error) {
        console.error(`Failed to parse workflow definition for workflow ${workflow.id}:`, error);

        return {};
    }

    const componentVersions: Record<string, number> = {};

    const collectFromNodes = (nodes: unknown) => {
        if (!Array.isArray(nodes)) {
            return;
        }

        for (const node of nodes) {
            const type = (node as {type?: string})?.type;

            if (!type) {
                continue;
            }

            const [componentName, versionToken] = type.split('/');

            if (componentName && versionToken && versionToken.startsWith('v')) {
                const version = Number.parseInt(versionToken.slice(1), 10);

                if (!Number.isNaN(version)) {
                    componentVersions[componentName] = version;
                }
            }
        }
    };

    collectFromNodes(definition.tasks);
    collectFromNodes(definition.triggers);

    return componentVersions;
}
