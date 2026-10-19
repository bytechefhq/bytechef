import {ClusterElementDefinitionApi, ComponentDefinition, Workflow} from '@/shared/middleware/platform/configuration';
import {ClusterElementDefinitionKeys} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {DEFINITION_STALE_TIME} from '@/shared/queries/queryConstants';
import {ClusterElementsType, PropertyAllType, UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

import {
    convertNameToSnakeCase,
    initializeClusterElementsObject,
} from '../../cluster-element-editor/utils/clusterElementsUtils';
import getFormattedName from './getFormattedName';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import {getTask} from './getTask';
import handleComponentAddedSuccess from './handleComponentAddedSuccess';
import processClusterElementsHierarchy from './processClusterElementsHierarchy';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface AddClusterElementToClusterRootProps {
    clusterElementName: string;
    clusterElementType: string;
    clusterRootComponentDefinition: ComponentDefinition;
    clusterRootId: string;
    componentDefinition: Pick<ComponentDefinition, 'clusterRoot' | 'name' | 'version'>;
    isMultipleElements: boolean;
    label?: string;
    queryClient: QueryClient;
    sourceNodeId: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow;
}

export default async function addClusterElementToClusterRoot({
    clusterElementName,
    clusterElementType,
    clusterRootComponentDefinition,
    clusterRootId,
    componentDefinition,
    isMultipleElements,
    label,
    queryClient,
    sourceNodeId,
    updateWorkflowMutation,
    workflow,
}: AddClusterElementToClusterRootProps): Promise<ClusterElementsType | undefined> {
    if (!workflow.definition) {
        return undefined;
    }

    const clusterRootTask = getTask({
        tasks: JSON.parse(workflow.definition).tasks,
        workflowNodeName: clusterRootId,
    });

    if (!clusterRootTask) {
        console.error(`Cannot find the cluster root task ${clusterRootId}`);

        return undefined;
    }

    const getClusterElementDefinitionRequest = {
        clusterElementName,
        clusterElementType: convertNameToSnakeCase(clusterElementType),
        componentName: componentDefinition.name,
        componentVersion: componentDefinition.version,
    };

    const clusterElementDefinition = await queryClient.query({
        queryFn: () =>
            new ClusterElementDefinitionApi().getComponentClusterElementDefinition(getClusterElementDefinitionRequest),
        queryKey: ClusterElementDefinitionKeys.clusterElementDefinition(getClusterElementDefinitionRequest),
        staleTime: DEFINITION_STALE_TIME,
    });

    const clusterElements = initializeClusterElementsObject({
        clusterElementsData: clusterRootTask.clusterElements || {},
        mainClusterRootComponentDefinition: clusterRootComponentDefinition,
        mainClusterRootTask: clusterRootTask,
    });

    const {nestedClusterElements} = processClusterElementsHierarchy({
        clusterElementData: {
            clusterElements: componentDefinition.clusterRoot ? {} : undefined,
            label,
            metadata: {},
            name: getFormattedName(componentDefinition.name),
            parameters:
                getParametersWithDefaultValues({
                    properties: clusterElementDefinition?.properties as Array<PropertyAllType>,
                }) || {},
            type: `${componentDefinition.name}/v${componentDefinition.version}/${clusterElementName}`,
        },
        clusterElements,
        elementType: clusterElementType,
        isMultipleElements,
        mainRootId: clusterRootId,
        sourceNodeId,
    });

    const clusterRootNodeData = {
        ...clusterRootTask,
        clusterElements: nestedClusterElements,
        componentName: clusterRootTask.type.split('/')[0],
        workflowNodeName: clusterRootId,
    };

    void saveWorkflowDefinition({
        nodeData: clusterRootNodeData,
        onSuccess: () => handleComponentAddedSuccess({nodeData: clusterRootNodeData, queryClient, workflow}),
        updateWorkflowMutation,
    });

    return nestedClusterElements;
}
