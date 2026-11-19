import {Workflow} from '@/shared/middleware/platform/configuration';

import {getWorkflowDefinitionNodes} from './getWorkflowDefinitionNodes';

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

    for (const node of getWorkflowDefinitionNodes(definition)) {
        const [componentName, versionToken] = node.type.split('/');

        if (!componentName || !versionToken || !versionToken.startsWith('v')) {
            continue;
        }

        const version = Number.parseInt(versionToken.slice(1), 10);

        if (Number.isNaN(version)) {
            continue;
        }

        const collectedVersion = componentVersions[componentName];

        componentVersions[componentName] =
            collectedVersion === undefined ? version : Math.max(collectedVersion, version);
    }

    return componentVersions;
}
