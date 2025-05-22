import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';

export default function getFormattedName(itemName: string): string {
    const {nodes, workflow} = useWorkflowDataStore.getState();

    const nodeNames = nodes.map((node) => (node.data as NodeDataType).name);

    const existingNodes = nodeNames.filter((name) => name?.includes(itemName));

    const workflowDefinition = JSON.parse(workflow.definition!);

    const clusterElementNames = workflowDefinition.tasks.map((task: WorkflowTask) => {
        const elementNames: string[] = [];

        const {clusterElements} = task;

        if (clusterElements) {
            Object.keys(clusterElements).forEach((elementType) => {
                const elements = clusterElements[elementType];

                if (Array.isArray(elements)) {
                    elements.forEach((element) => {
                        if (element.name?.includes(itemName)) {
                            elementNames.push(element.name);
                        }
                    });
                } else if (elements?.name?.includes(itemName)) {
                    elementNames.push(elements.name);
                }
            });
        }

        return elementNames;
    });

    const existingClusterElementNodes = clusterElementNames.flatMap((names: string[] | string) => {
        if (Array.isArray(names)) {
            return names.filter((name: string) => name?.includes(itemName));
        }

        return names?.includes(itemName) ? [names] : [];
    });

    const allExistingNodes = [...existingNodes, ...existingClusterElementNodes];

    if (!allExistingNodes.length) {
        return `${itemName}_1`;
    }

    const allNumbers = allExistingNodes.map((name: string) => {
        const nodeNameSplit = name.split('_');
        const lastSegmentIndex = nodeNameSplit.length - 1;

        return parseInt(nodeNameSplit[lastSegmentIndex]);
    });

    const maxExistingNumber = Math.max(...allNumbers);

    return `${itemName}_${maxExistingNumber + 1}`;
}
