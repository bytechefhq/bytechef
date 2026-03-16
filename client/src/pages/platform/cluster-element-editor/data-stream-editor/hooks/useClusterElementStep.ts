import {
    convertNameToSnakeCase,
    initializeClusterElementsObject,
} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import getFormattedName from '@/pages/platform/workflow-editor/utils/getFormattedName';
import getParametersWithDefaultValues from '@/pages/platform/workflow-editor/utils/getParametersWithDefaultValues';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import handleComponentAddedSuccess from '@/pages/platform/workflow-editor/utils/handleComponentAddedSuccess';
import processClusterElementsHierarchy from '@/pages/platform/workflow-editor/utils/processClusterElementsHierarchy';
import saveWorkflowDefinition from '@/pages/platform/workflow-editor/utils/saveWorkflowDefinition';
import {ClusterElementDefinitionApi, ComponentConnection} from '@/shared/middleware/platform/configuration';
import {ClusterElementDefinitionKeys} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetClusterElementParameterDisplayConditionsQuery} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {useShallow} from 'zustand/shallow';

type ClusterElementStepType = 'destination' | 'source';

export interface ClusterElementStepItemI {
    componentName: string;
    icon?: string;
    label: string;
    name: string;
    operationName: string;
    title: string;
    type: string;
}

export default function useClusterElementStep(elementType: ClusterElementStepType) {
    const [selectedComponentName, setSelectedComponentName] = useState<string>('');
    const [selectedOperationName, setSelectedOperationName] = useState<string>('');
    const [elementProperties, setElementProperties] = useState<PropertyAllType[]>([]);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {mainClusterRootComponentDefinition, rootClusterElementNodeData, setRootClusterElementNodeData} =
        useWorkflowEditorStore(
            useShallow((state) => ({
                mainClusterRootComponentDefinition: state.mainClusterRootComponentDefinition,
                rootClusterElementNodeData: state.rootClusterElementNodeData,
                setRootClusterElementNodeData: state.setRootClusterElementNodeData,
            }))
        );

    const componentDefinitions = useWorkflowDataStore((state) => state.componentDefinitions);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {currentNode, setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentNode: state.currentNode,
            setCurrentComponent: state.setCurrentComponent,
            setCurrentNode: state.setCurrentNode,
        }))
    );

    const {updateWorkflowMutation} = useWorkflowEditor();

    const queryClient = useQueryClient();

    const elementTypeKey = useMemo(() => convertNameToSnakeCase(elementType), [elementType]);

    const elementItem = useMemo<ClusterElementStepItemI | null>(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return null;
        }

        const elementValue = clusterElements[elementType];

        if (!elementValue) {
            return null;
        }

        const element = (Array.isArray(elementValue) ? elementValue[0] : elementValue) as unknown as NodeDataType;
        const typeSegments = element.type?.split('/') || [];
        const componentName = element.componentName || typeSegments[0] || '';
        const operationName = element.operationName || typeSegments[2] || '';
        const definitionsMap = new Map(componentDefinitions.map((definition) => [definition.name, definition]));

        const componentDefinition = definitionsMap.get(componentName);

        const workflowNodeName = element.workflowNodeName || element.name || '';

        return {
            componentName,
            icon: componentDefinition?.icon,
            label: element.label || workflowNodeName,
            name: workflowNodeName,
            operationName,
            title: componentDefinition?.title || componentName,
            type: element.type || '',
        };
    }, [rootClusterElementNodeData?.clusterElements, componentDefinitions, elementType]);

    const elementComponentVersion = useMemo(() => {
        if (!elementItem?.type) {
            return 1;
        }

        return Number(elementItem.type.split('/')[1]?.replace(/^v/, '')) || 1;
    }, [elementItem?.type]);

    const stepComponentDefinitions = useMemo(() => {
        return componentDefinitions.filter((definition) => {
            return (
                definition.clusterElementsCount?.[elementTypeKey] && definition.clusterElementsCount[elementTypeKey] > 0
            );
        });
    }, [componentDefinitions, elementTypeKey]);

    const {data: selectedFullComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: selectedComponentName,
            componentVersion:
                stepComponentDefinitions.find((definition) => definition.name === selectedComponentName)?.version || 1,
        },
        !!selectedComponentName
    );

    const {data: elementComponentDefinition} = useGetComponentDefinitionQuery(
        {componentName: elementItem?.componentName || '', componentVersion: elementComponentVersion},
        !!elementItem?.componentName
    );

    const stepOperations = useMemo(() => {
        if (!selectedFullComponentDefinition?.clusterElements) {
            return [];
        }

        return selectedFullComponentDefinition.clusterElements.filter((clusterElement) => {
            return clusterElement.type === elementTypeKey;
        });
    }, [selectedFullComponentDefinition?.clusterElements, elementTypeKey]);

    const {data: testConnections} = useGetWorkflowTestConfigurationConnectionsQuery(
        {
            environmentId: currentEnvironmentId,
            workflowId: workflow.id!,
            workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
        },
        !!workflow.id && !!rootClusterElementNodeData?.workflowNodeName
    );

    const displayConditionsQuery = useGetClusterElementParameterDisplayConditionsQuery(
        {
            clusterElementType: elementTypeKey,
            clusterElementWorkflowNodeName: elementItem?.name || '',
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
        },
        !!elementItem && !!workflow.id && !!rootClusterElementNodeData?.workflowNodeName
    );

    const componentConnections = useMemo<ComponentConnection[]>(() => {
        if (!elementItem || !elementComponentDefinition?.connection) {
            return [];
        }

        return [
            {
                componentName: elementItem.componentName,
                componentVersion: elementComponentVersion,
                key: elementItem.name,
                required: elementComponentDefinition.connectionRequired || false,
                workflowNodeName: elementItem.name,
            },
        ];
    }, [
        elementItem,
        elementComponentDefinition?.connection,
        elementComponentDefinition?.connectionRequired,
        elementComponentVersion,
    ]);

    const setupNodeDetailsPanel = useCallback(
        (item: ClusterElementStepItemI) => {
            const typeSegments = item.type.split('/');
            const clusterElementName = typeSegments[2];
            const componentVersion = parseInt(typeSegments[1]?.replace(/^v/, '')) || 1;

            const nodeData: NodeDataType = {
                clusterElementName,
                clusterElementType: elementType,
                componentName: item.componentName,
                description: '',
                label: item.label,
                name: item.name,
                operationName: item.operationName,
                parentClusterRootId: rootClusterElementNodeData?.name,
                type: item.type,
                version: componentVersion,
                workflowNodeName: item.name,
            };

            setCurrentNode(nodeData);

            setCurrentComponent((previousCurrentComponent) => ({
                ...nodeData,
                displayConditions: previousCurrentComponent?.displayConditions,
                workflowNodeName: item.name,
            }));
        },
        [elementType, rootClusterElementNodeData?.name, setCurrentComponent, setCurrentNode]
    );

    const handleComponentChange = useCallback((componentName: string) => {
        setSelectedComponentName(componentName);
        setSelectedOperationName('');
        setElementProperties([]);
    }, []);

    const handleOperationChange = useCallback(
        async (operationName: string) => {
            if (!selectedFullComponentDefinition || !operationName) {
                return;
            }

            setSelectedOperationName(operationName);

            const componentName = selectedFullComponentDefinition.name;
            const componentTitle = selectedFullComponentDefinition.title || componentName;
            const version = selectedFullComponentDefinition.version;

            const getClusterElementDefinitionRequest = {
                clusterElementName: operationName,
                clusterElementType: elementType.toUpperCase(),
                componentName,
                componentVersion: version,
            };

            const clusterElementDefinition = await queryClient.fetchQuery({
                queryFn: () =>
                    new ClusterElementDefinitionApi().getComponentClusterElementDefinition(
                        getClusterElementDefinitionRequest
                    ),
                queryKey: ClusterElementDefinitionKeys.clusterElementDefinition(getClusterElementDefinitionRequest),
            });

            const clusterElementData = {
                label: componentTitle,
                metadata: {},
                name: getFormattedName(componentName),
                parameters:
                    getParametersWithDefaultValues({
                        properties: (clusterElementDefinition?.properties as PropertyAllType[]) || [],
                    }) || {},
                type: `${componentName}/v${version}/${operationName}`,
            };

            if (!workflow.definition || !mainClusterRootComponentDefinition) {
                return;
            }

            if (!rootClusterElementNodeData?.workflowNodeName || !rootClusterElementNodeData?.componentName) {
                console.error('Root cluster element node data is missing required properties');

                return;
            }

            const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

            const mainClusterRootTask = getTask({
                tasks: workflowDefinitionTasks,
                workflowNodeName: rootClusterElementNodeData.workflowNodeName,
            });

            if (!mainClusterRootTask) {
                return;
            }

            const clusterElements = initializeClusterElementsObject({
                clusterElementsData: mainClusterRootTask.clusterElements || {},
                mainClusterRootComponentDefinition,
                mainClusterRootTask,
            });

            const updatedClusterElements = processClusterElementsHierarchy({
                clusterElementData,
                clusterElements,
                elementType,
                isMultipleElements: false,
                mainRootId: rootClusterElementNodeData.workflowNodeName,
                sourceNodeId: rootClusterElementNodeData.workflowNodeName,
            });

            const updatedNodeData = {
                ...mainClusterRootTask,
                clusterElements: updatedClusterElements.nestedClusterElements,
            };

            setRootClusterElementNodeData({
                ...rootClusterElementNodeData,
                clusterElements: updatedClusterElements.nestedClusterElements,
            } as typeof rootClusterElementNodeData);

            if (currentNode?.clusterRoot && !currentNode.isNestedClusterRoot) {
                setCurrentNode({
                    ...currentNode,
                    clusterElements: updatedClusterElements.nestedClusterElements,
                });
            }

            saveWorkflowDefinition({
                nodeData: {
                    ...updatedNodeData,
                    componentName: rootClusterElementNodeData.componentName,
                    workflowNodeName: rootClusterElementNodeData.workflowNodeName,
                },
                onSuccess: () => {
                    handleComponentAddedSuccess({
                        nodeData: {
                            ...updatedNodeData,
                            componentName: rootClusterElementNodeData.componentName,
                            workflowNodeName: rootClusterElementNodeData.workflowNodeName,
                        },
                        queryClient,
                        workflow,
                    });
                },
                updateWorkflowMutation: updateWorkflowMutation!,
            });

            setElementProperties((clusterElementDefinition.properties as PropertyAllType[]) || []);
        },
        [
            currentNode,
            elementType,
            mainClusterRootComponentDefinition,
            queryClient,
            rootClusterElementNodeData,
            selectedFullComponentDefinition,
            setCurrentNode,
            setRootClusterElementNodeData,
            updateWorkflowMutation,
            workflow,
        ]
    );

    useEffect(() => {
        if (elementItem) {
            setSelectedComponentName(elementItem.componentName);
            setSelectedOperationName(elementItem.operationName);
        }
    }, [elementItem]);

    useEffect(() => {
        if (!elementItem) {
            setElementProperties([]);

            return;
        }

        const typeSegments = elementItem.type.split('/');
        const clusterElementName = typeSegments[2];
        const componentVersion = parseInt(typeSegments[1]?.replace(/^v/, '')) || 1;

        const fetchProperties = async () => {
            try {
                const definition = await queryClient.fetchQuery({
                    queryFn: () =>
                        new ClusterElementDefinitionApi().getComponentClusterElementDefinition({
                            clusterElementName,
                            clusterElementType: elementType.toUpperCase(),
                            componentName: elementItem.componentName,
                            componentVersion: componentVersion,
                        }),
                    queryKey: ClusterElementDefinitionKeys.clusterElementDefinition({
                        clusterElementName,
                        clusterElementType: elementType.toUpperCase(),
                        componentName: elementItem.componentName,
                        componentVersion: componentVersion,
                    }),
                });

                setElementProperties((definition.properties as PropertyAllType[]) || []);
            } catch (error) {
                console.warn('Failed to fetch cluster element definition for properties:', error);

                setElementProperties([]);
            }
        };

        fetchProperties();
        setupNodeDetailsPanel(elementItem);
    }, [elementItem, elementType, queryClient, setupNodeDetailsPanel]);

    return {
        componentConnections,
        displayConditionsQuery,
        elementComponentDefinition,
        elementItem,
        elementProperties,
        handleComponentChange,
        handleOperationChange,
        rootWorkflowNodeName: rootClusterElementNodeData?.workflowNodeName,
        selectedComponentName,
        selectedOperationName,
        stepComponentDefinitions,
        stepOperations,
        testConnections,
        workflowId: workflow.id,
    };
}
