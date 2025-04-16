import {WorkflowTask} from '@/shared/middleware/platform/configuration';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';

export default function getFormattedClusterElementName(clusterElementName: string, clusterElementType: string) {
    const {workflow} = useWorkflowDataStore.getState();

    const workflowDefinition = JSON.parse(workflow.definition!);

    const clusterElementNames = workflowDefinition.tasks.map((task: WorkflowTask) => {
        let elementName = [];
        if (task.clusterElements && task.clusterElements[clusterElementType]) {
            if (clusterElementType === 'tools') {
                elementName = task.clusterElements.tools.map((tool: WorkflowTask) => {
                    return tool.name;
                });
            } else {
                elementName = task.clusterElements[clusterElementType].name;
            }
        }

        return elementName;
    });

    const existingClusterElements = clusterElementNames.flatMap((names: string[]) => {
        if (clusterElementType === 'tools') {
            return names.filter((name: string) => name?.includes(clusterElementName));
        }

        return names?.includes(clusterElementName) ? [names] : [];
    });

    if (!existingClusterElements.length) {
        return `${clusterElementName}_1`;
    }

    const existingClusterElementsNumbers = existingClusterElements.map((name: string) => {
        const nodeNameSplit = name.split('_');

        return parseInt(nodeNameSplit[nodeNameSplit.length - 1]);
    });

    const maxExistingClusterElementNumber = Math.max(...existingClusterElementsNumbers);

    return `${clusterElementName}_${maxExistingClusterElementNumber + 1}`;
}
