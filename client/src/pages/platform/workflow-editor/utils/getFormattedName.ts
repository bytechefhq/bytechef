import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {ClusterElementsType, NodeDataType} from '@/shared/types';

import {isPlainObject} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';

export default function getFormattedName(itemName: string): string {
    const {nodes, workflow} = useWorkflowDataStore.getState();

    const nodeNames = nodes.map((node) => (node.data as NodeDataType).name);

    const existingNodes = nodeNames.filter((name) => name?.includes(itemName));

    const workflowDefinition = JSON.parse(workflow.definition!);

    const clusterElementNames = workflowDefinition.tasks.map((task: WorkflowTask) => {
        const elementNames: string[] = [];

        const {clusterElements} = task;

        const getClusterElementNames = (elements: ClusterElementsType, names: string[]) => {
            if (!elements) {
                return;
            }

            Object.keys(elements).forEach((elementType) => {
                const typeElements = elements[elementType];

                if (Array.isArray(typeElements)) {
                    typeElements.forEach((element) => {
                        if (element.name?.includes(itemName)) {
                            names.push(element.name);
                        }

                        if (element.clusterElements) {
                            getClusterElementNames(element.clusterElements, names);
                        }
                    });
                } else if (isPlainObject(typeElements)) {
                    if (typeElements.name?.includes(itemName)) {
                        names.push(typeElements.name);
                    }

                    if (typeElements.clusterElements) {
                        getClusterElementNames(typeElements.clusterElements, names);
                    }
                }
            });
        };

        if (clusterElements) {
            getClusterElementNames(clusterElements, elementNames);
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
