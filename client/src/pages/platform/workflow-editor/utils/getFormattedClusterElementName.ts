import {WorkflowTask} from '@/shared/middleware/platform/configuration';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';

export default function getFormattedClusterElementName(clusterElementName: string, clusterElementType: string) {
    const {workflow} = useWorkflowDataStore.getState();

    const workflowDefinition = JSON.parse(workflow.definition!);

    const clusterElementNames = workflowDefinition.tasks.map((task: WorkflowTask) => {
        let elementName = [];

        const {clusterElements} = task;

        if (clusterElements && clusterElements[clusterElementType]) {
            if (clusterElementType === 'tools') {
                elementName = clusterElements.tools.map((tool: WorkflowTask) => tool.name);
            } else {
                elementName = clusterElements[clusterElementType].name;
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
        const lastSegmentIndex = nodeNameSplit.length - 1;

        return parseInt(nodeNameSplit[lastSegmentIndex]);
    });

    const maxExistingClusterElementNumber = Math.max(...existingClusterElementsNumbers);

    return `${clusterElementName}_${maxExistingClusterElementNumber + 1}`;
}
