import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {
    convertNameToSnakeCase,
    initializeClusterElementsObject,
} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import ConnectionTab from '@/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTab';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
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

interface DestinationItemI {
    componentName: string;
    icon?: string;
    label: string;
    name: string;
    operationName: string;
    title: string;
    type: string;
}

export default function DataStreamDestinationStep() {
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

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const queryClient = useQueryClient();

    const [selectedComponentName, setSelectedComponentName] = useState<string>('');
    const [selectedOperationName, setSelectedOperationName] = useState<string>('');
    const [destinationProperties, setDestinationProperties] = useState<PropertyAllType[]>([]);

    // Derive current destination from root cluster element node data
    const destination = useMemo<DestinationItemI | null>(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return null;
        }

        const destinationValue = clusterElements['destination'];

        if (!destinationValue) {
            return null;
        }

        const destinationElement = (Array.isArray(destinationValue)
            ? destinationValue[0]
            : destinationValue) as unknown as NodeDataType;
        const typeSegments = destinationElement.type?.split('/') || [];
        const componentName = destinationElement.componentName || typeSegments[0] || '';
        const operationName = destinationElement.operationName || typeSegments[2] || '';
        const definitionsMap = new Map(componentDefinitions.map((definition) => [definition.name, definition]));
        const componentDefinition = definitionsMap.get(componentName);

        return {
            componentName,
            icon: componentDefinition?.icon,
            label: destinationElement.label || destinationElement.workflowNodeName || '',
            name: destinationElement.workflowNodeName || '',
            operationName,
            title: componentDefinition?.title || componentName,
            type: destinationElement.type || '',
        };
    }, [rootClusterElementNodeData?.clusterElements, componentDefinitions]);

    const destinationComponentVersion = useMemo(() => {
        if (!destination?.type) {
            return 1;
        }

        return Number(destination.type.split('/')[1]?.replace(/^v/, '')) || 1;
    }, [destination?.type]);

    // Sync local state from existing destination
    useEffect(() => {
        if (destination) {
            setSelectedComponentName(destination.componentName);
            setSelectedOperationName(destination.operationName);
        }
    }, [destination]);

    // Filter componentDefinitions to only those that have DESTINATION cluster elements
    const destinationComponentDefinitions = useMemo(() => {
        return componentDefinitions.filter((definition) => {
            return (
                definition.clusterElementsCount?.['DESTINATION'] && definition.clusterElementsCount['DESTINATION'] > 0
            );
        });
    }, [componentDefinitions]);

    // Fetch the full ComponentDefinition for the selected component
    const {data: selectedFullComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: selectedComponentName,
            componentVersion:
                destinationComponentDefinitions.find((definition) => definition.name === selectedComponentName)
                    ?.version || 1,
        },
        !!selectedComponentName
    );

    // Fetch the destination component definition (for already-saved destination)
    const {data: destinationComponentDefinition} = useGetComponentDefinitionQuery(
        {componentName: destination?.componentName || '', componentVersion: destinationComponentVersion},
        !!destination?.componentName
    );

    // Destination operations filtered by type === 'DESTINATION'
    const destinationOperations = useMemo(() => {
        if (!selectedFullComponentDefinition?.clusterElements) {
            return [];
        }

        return selectedFullComponentDefinition.clusterElements.filter((clusterElement) => {
            return clusterElement.type === convertNameToSnakeCase('destination');
        });
    }, [selectedFullComponentDefinition?.clusterElements]);

    const {data: testConnections} = useGetWorkflowTestConfigurationConnectionsQuery(
        {
            environmentId: currentEnvironmentId,
            workflowId: workflow.id!,
            workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
        },
        !!workflow.id && !!rootClusterElementNodeData?.workflowNodeName
    );

    // Display conditions query for Properties
    const displayConditionsQuery = useGetClusterElementParameterDisplayConditionsQuery(
        {
            clusterElementType: 'destination',
            clusterElementWorkflowNodeName: destination?.name || '',
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
        },
        !!destination && !!workflow.id && !!rootClusterElementNodeData?.workflowNodeName
    );

    // Build connection objects for ConnectionTab
    const destinationConnections = useMemo<ComponentConnection[]>(() => {
        if (!destination || !destinationComponentDefinition?.connection) {
            return [];
        }

        return [
            {
                componentName: destination.componentName,
                componentVersion: destinationComponentVersion,
                key: destination.name,
                required: destinationComponentDefinition.connectionRequired || false,
                workflowNodeName: destination.name,
            },
        ];
    }, [
        destination,
        destinationComponentDefinition?.connection,
        destinationComponentDefinition?.connectionRequired,
        destinationComponentVersion,
    ]);

    // Set currentNode and currentComponent when destination exists so Properties works
    const setupNodeDetailsPanelForDestination = useCallback(
        (destinationItem: DestinationItemI) => {
            const typeSegments = destinationItem.type.split('/');
            const clusterElementName = typeSegments[2];
            const componentVersion = parseInt(typeSegments[1]?.replace(/^v/, '')) || 1;

            const destinationNodeData: NodeDataType = {
                clusterElementName,
                clusterElementType: 'destination',
                componentName: destinationItem.componentName,
                description: '',
                label: destinationItem.label,
                name: destinationItem.name,
                operationName: destinationItem.operationName,
                parentClusterRootId: rootClusterElementNodeData?.name,
                type: destinationItem.type,
                version: componentVersion,
                workflowNodeName: destinationItem.name,
            };

            setCurrentNode(destinationNodeData);

            setCurrentComponent((previousCurrentComponent) => ({
                ...destinationNodeData,
                displayConditions: previousCurrentComponent?.displayConditions,
                workflowNodeName: destinationItem.name,
            }));
        },
        [rootClusterElementNodeData?.name, setCurrentComponent, setCurrentNode]
    );

    // Fetch and set destination properties when destination is available
    useEffect(() => {
        if (!destination) {
            setDestinationProperties([]);

            return;
        }

        const typeSegments = destination.type.split('/');
        const clusterElementName = typeSegments[2];
        const componentVersion = parseInt(typeSegments[1]?.replace(/^v/, '')) || 1;

        const fetchProperties = async () => {
            try {
                const definition = await queryClient.fetchQuery({
                    queryFn: () =>
                        new ClusterElementDefinitionApi().getComponentClusterElementDefinition({
                            clusterElementName,
                            componentName: destination.componentName,
                            componentVersion: componentVersion,
                        }),
                    queryKey: ClusterElementDefinitionKeys.clusterElementDefinition({
                        clusterElementName,
                        componentName: destination.componentName,
                        componentVersion: componentVersion,
                    }),
                });

                setDestinationProperties((definition.properties as PropertyAllType[]) || []);
            } catch (error) {
                console.warn('Failed to fetch cluster element definition for properties:', error);

                setDestinationProperties([]);
            }
        };

        fetchProperties();
        setupNodeDetailsPanelForDestination(destination);
    }, [destination, queryClient, setupNodeDetailsPanelForDestination]);

    // Handle component selection change
    const handleComponentChange = useCallback((componentName: string) => {
        setSelectedComponentName(componentName);
        setSelectedOperationName('');
        setDestinationProperties([]);
    }, []);

    // Handle operation selection - this saves the cluster element to the workflow
    const handleOperationChange = useCallback(
        async (operationName: string) => {
            if (!selectedFullComponentDefinition || !operationName) {
                return;
            }

            setSelectedOperationName(operationName);

            const componentName = selectedFullComponentDefinition.name;
            const componentTitle = selectedFullComponentDefinition.title || componentName;
            const version = selectedFullComponentDefinition.version;

            // Fetch the ClusterElementDefinition from API
            const getClusterElementDefinitionRequest = {
                clusterElementName: operationName,
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

            // Build cluster element data
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

            // Save cluster element to workflow
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
                elementType: 'destination',
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
                invalidateWorkflowQueries,
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

            // Set properties from the fetched definition
            setDestinationProperties((clusterElementDefinition.properties as PropertyAllType[]) || []);
        },
        [
            currentNode,
            invalidateWorkflowQueries,
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

    const rootWorkflowNodeName = rootClusterElementNodeData?.workflowNodeName;

    return (
        <div className="space-y-4 py-4">
            <div>
                <h2 className="text-lg font-semibold">Select Destination</h2>

                <p className="text-sm text-muted-foreground">
                    Choose a data destination component and configure its connection and parameters.
                </p>
            </div>

            {rootWorkflowNodeName && (
                <>
                    <fieldset className="flex flex-col gap-2 border-0 p-0">
                        <label className="text-sm font-medium" htmlFor="destination-component-select">
                            Component
                        </label>

                        <Select onValueChange={handleComponentChange} value={selectedComponentName}>
                            <SelectTrigger id="destination-component-select">
                                <SelectValue placeholder="Select a destination component..." />
                            </SelectTrigger>

                            <SelectContent>
                                {destinationComponentDefinitions.map((definition) => (
                                    <SelectItem key={definition.name} value={definition.name}>
                                        {definition.title || definition.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    {selectedComponentName && destinationOperations.length > 0 && (
                        <fieldset className="flex flex-col gap-2 border-0 p-0">
                            <label className="text-sm font-medium" htmlFor="destination-operation-select">
                                Operation
                            </label>

                            <Select onValueChange={handleOperationChange} value={selectedOperationName}>
                                <SelectTrigger id="destination-operation-select">
                                    <SelectValue placeholder="Select an operation..." />
                                </SelectTrigger>

                                <SelectContent>
                                    {destinationOperations.map((operation) => (
                                        <SelectItem key={operation.name} value={operation.name}>
                                            {operation.title || operation.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </fieldset>
                    )}

                    {destination && destinationComponentDefinition?.connection && (
                        <div className="flex flex-col gap-2">
                            <h3 className="text-sm font-medium">Connection</h3>

                            <ConnectionTab
                                componentConnections={destinationConnections}
                                currentComponentDefinition={destinationComponentDefinition}
                                workflowId={workflow.id!}
                                workflowNodeName={destination.name}
                                workflowTestConfigurationConnections={testConnections}
                            />
                        </div>
                    )}

                    {destination && destinationProperties.length > 0 && (
                        <div className="flex flex-col gap-2">
                            <h3 className="text-sm font-medium">Properties</h3>

                            <Properties
                                customClassName="p-0"
                                displayConditionsQuery={displayConditionsQuery}
                                operationName={destination.operationName}
                                properties={destinationProperties}
                            />
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
