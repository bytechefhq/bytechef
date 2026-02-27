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

interface SourceItemI {
    componentName: string;
    icon?: string;
    label: string;
    name: string;
    operationName: string;
    title: string;
    type: string;
}

export default function DataStreamSourceStep() {
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
    const [sourceProperties, setSourceProperties] = useState<PropertyAllType[]>([]);

    // Derive current source from root cluster element node data
    const source = useMemo<SourceItemI | null>(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return null;
        }

        const sourceValue = clusterElements['source'];

        if (!sourceValue) {
            return null;
        }

        const sourceElement = (Array.isArray(sourceValue) ? sourceValue[0] : sourceValue) as unknown as NodeDataType;
        const typeSegments = sourceElement.type?.split('/') || [];
        const componentName = sourceElement.componentName || typeSegments[0] || '';
        const operationName = sourceElement.operationName || typeSegments[2] || '';
        const definitionsMap = new Map(componentDefinitions.map((definition) => [definition.name, definition]));
        const componentDefinition = definitionsMap.get(componentName);

        return {
            componentName,
            icon: componentDefinition?.icon,
            label: sourceElement.label || sourceElement.workflowNodeName || '',
            name: sourceElement.workflowNodeName || '',
            operationName,
            title: componentDefinition?.title || componentName,
            type: sourceElement.type || '',
        };
    }, [rootClusterElementNodeData?.clusterElements, componentDefinitions]);

    const sourceComponentVersion = useMemo(() => {
        if (!source?.type) {
            return 1;
        }

        return Number(source.type.split('/')[1]?.replace(/^v/, '')) || 1;
    }, [source?.type]);

    // Sync local state from existing source
    useEffect(() => {
        if (source) {
            setSelectedComponentName(source.componentName);
            setSelectedOperationName(source.operationName);
        }
    }, [source]);

    // Filter componentDefinitions to only those that have SOURCE cluster elements
    const sourceComponentDefinitions = useMemo(() => {
        return componentDefinitions.filter((definition) => {
            return definition.clusterElementsCount?.['SOURCE'] && definition.clusterElementsCount['SOURCE'] > 0;
        });
    }, [componentDefinitions]);

    // Fetch the full ComponentDefinition for the selected component
    const {data: selectedFullComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: selectedComponentName,
            componentVersion:
                sourceComponentDefinitions.find((definition) => definition.name === selectedComponentName)?.version ||
                1,
        },
        !!selectedComponentName
    );

    // Fetch the source component definition (for already-saved source)
    const {data: sourceComponentDefinition} = useGetComponentDefinitionQuery(
        {componentName: source?.componentName || '', componentVersion: sourceComponentVersion},
        !!source?.componentName
    );

    // Source operations filtered by type === 'SOURCE'
    const sourceOperations = useMemo(() => {
        if (!selectedFullComponentDefinition?.clusterElements) {
            return [];
        }

        return selectedFullComponentDefinition.clusterElements.filter((clusterElement) => {
            return clusterElement.type === convertNameToSnakeCase('source');
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
            clusterElementType: 'source',
            clusterElementWorkflowNodeName: source?.name || '',
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
        },
        !!source && !!workflow.id && !!rootClusterElementNodeData?.workflowNodeName
    );

    // Build connection objects for ConnectionTab
    const sourceConnections = useMemo<ComponentConnection[]>(() => {
        if (!source || !sourceComponentDefinition?.connection) {
            return [];
        }

        return [
            {
                componentName: source.componentName,
                componentVersion: sourceComponentVersion,
                key: source.name,
                required: sourceComponentDefinition.connectionRequired || false,
                workflowNodeName: source.name,
            },
        ];
    }, [
        source,
        sourceComponentDefinition?.connection,
        sourceComponentDefinition?.connectionRequired,
        sourceComponentVersion,
    ]);

    // Set currentNode and currentComponent when source exists so Properties works
    const setupNodeDetailsPanelForSource = useCallback(
        (sourceItem: SourceItemI) => {
            const typeSegments = sourceItem.type.split('/');
            const clusterElementName = typeSegments[2];
            const componentVersion = parseInt(typeSegments[1]?.replace(/^v/, '')) || 1;

            const sourceNodeData: NodeDataType = {
                clusterElementName,
                clusterElementType: 'source',
                componentName: sourceItem.componentName,
                description: '',
                label: sourceItem.label,
                name: sourceItem.name,
                operationName: sourceItem.operationName,
                parentClusterRootId: rootClusterElementNodeData?.name,
                type: sourceItem.type,
                version: componentVersion,
                workflowNodeName: sourceItem.name,
            };

            setCurrentNode(sourceNodeData);

            setCurrentComponent((previousCurrentComponent) => ({
                ...sourceNodeData,
                displayConditions: previousCurrentComponent?.displayConditions,
                workflowNodeName: sourceItem.name,
            }));
        },
        [rootClusterElementNodeData?.name, setCurrentComponent, setCurrentNode]
    );

    // Fetch and set source properties when source is available
    useEffect(() => {
        if (!source) {
            setSourceProperties([]);

            return;
        }

        const typeSegments = source.type.split('/');
        const clusterElementName = typeSegments[2];
        const componentVersion = parseInt(typeSegments[1]?.replace(/^v/, '')) || 1;

        const fetchProperties = async () => {
            try {
                const definition = await queryClient.fetchQuery({
                    queryFn: () =>
                        new ClusterElementDefinitionApi().getComponentClusterElementDefinition({
                            clusterElementName,
                            componentName: source.componentName,
                            componentVersion: componentVersion,
                        }),
                    queryKey: ClusterElementDefinitionKeys.clusterElementDefinition({
                        clusterElementName,
                        componentName: source.componentName,
                        componentVersion: componentVersion,
                    }),
                });

                setSourceProperties((definition.properties as PropertyAllType[]) || []);
            } catch (error) {
                console.warn('Failed to fetch cluster element definition for properties:', error);

                setSourceProperties([]);
            }
        };

        fetchProperties();
        setupNodeDetailsPanelForSource(source);
    }, [source, queryClient, setupNodeDetailsPanelForSource]);

    // Handle component selection change
    const handleComponentChange = useCallback((componentName: string) => {
        setSelectedComponentName(componentName);
        setSelectedOperationName('');
        setSourceProperties([]);
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
                elementType: 'source',
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
            setSourceProperties((clusterElementDefinition.properties as PropertyAllType[]) || []);
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
                <h2 className="text-lg font-semibold">Select Source</h2>

                <p className="text-sm text-muted-foreground">
                    Choose a data source component and configure its connection and parameters.
                </p>
            </div>

            {rootWorkflowNodeName && (
                <>
                    <fieldset className="flex flex-col gap-2 border-0 p-0">
                        <label className="text-sm font-medium" htmlFor="source-component-select">
                            Component
                        </label>

                        <Select onValueChange={handleComponentChange} value={selectedComponentName}>
                            <SelectTrigger id="source-component-select">
                                <SelectValue placeholder="Select a source component..." />
                            </SelectTrigger>

                            <SelectContent>
                                {sourceComponentDefinitions.map((definition) => (
                                    <SelectItem key={definition.name} value={definition.name}>
                                        {definition.title || definition.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </fieldset>

                    {selectedComponentName && sourceOperations.length > 0 && (
                        <fieldset className="flex flex-col gap-2 border-0 p-0">
                            <label className="text-sm font-medium" htmlFor="source-operation-select">
                                Operation
                            </label>

                            <Select onValueChange={handleOperationChange} value={selectedOperationName}>
                                <SelectTrigger id="source-operation-select">
                                    <SelectValue placeholder="Select an operation..." />
                                </SelectTrigger>

                                <SelectContent>
                                    {sourceOperations.map((operation) => (
                                        <SelectItem key={operation.name} value={operation.name}>
                                            {operation.title || operation.name}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </fieldset>
                    )}

                    {source && sourceComponentDefinition?.connection && (
                        <div className="flex flex-col gap-2">
                            <h3 className="text-sm font-medium">Connection</h3>

                            <ConnectionTab
                                componentConnections={sourceConnections}
                                currentComponentDefinition={sourceComponentDefinition}
                                workflowId={workflow.id!}
                                workflowNodeName={source.name}
                                workflowTestConfigurationConnections={testConnections}
                            />
                        </div>
                    )}

                    {source && sourceProperties.length > 0 && (
                        <div className="flex flex-col gap-2">
                            <h3 className="text-sm font-medium">Properties</h3>

                            <Properties
                                customClassName="p-0"
                                displayConditionsQuery={displayConditionsQuery}
                                operationName={source.operationName}
                                properties={sourceProperties}
                            />
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
