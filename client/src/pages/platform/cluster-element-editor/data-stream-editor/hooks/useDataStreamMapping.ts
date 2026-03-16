import {getClusterElementByName} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import {
    ClusterElementDefinitionApi,
    WorkflowNodeOptionApi,
    WorkflowNodeParameterApi,
} from '@/shared/middleware/platform/configuration';
import {ClusterElementDefinitionKeys} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useGetClusterElementParameterDisplayConditionsQuery} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {useShallow} from 'zustand/shallow';

interface ProcessorItemI {
    componentName: string;
    label: string;
    name: string;
    operationName: string;
    type: string;
}

export default function useDataStreamMapping() {
    const [autoMapping, setAutoMapping] = useState(false);
    const [processorProperties, setProcessorProperties] = useState<PropertyAllType[]>([]);
    const [propertiesKey, setPropertiesKey] = useState(0);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);

    const componentDefinitions = useWorkflowDataStore((state) => state.componentDefinitions);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            setCurrentComponent: state.setCurrentComponent,
            setCurrentNode: state.setCurrentNode,
        }))
    );

    const {invalidateWorkflowQueries} = useWorkflowEditor();

    const queryClient = useQueryClient();

    const processor = useMemo<ProcessorItemI | null>(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return null;
        }

        const processorValue = clusterElements['processor'];

        if (!processorValue) {
            return null;
        }

        const processorElement = (Array.isArray(processorValue)
            ? processorValue[0]
            : processorValue) as unknown as NodeDataType;

        const typeSegments = processorElement.type?.split('/') || [];

        const componentName = processorElement.componentName || typeSegments[0] || '';
        const operationName = processorElement.operationName || typeSegments[2] || '';

        return {
            componentName,
            label: processorElement.label || processorElement.workflowNodeName || '',
            name: processorElement.workflowNodeName || '',
            operationName,
            type: processorElement.type || '',
        };
    }, [rootClusterElementNodeData?.clusterElements]);

    const hasSourceAndDestination = useMemo(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return false;
        }

        return !!clusterElements['source'] && !!clusterElements['destination'];
    }, [rootClusterElementNodeData?.clusterElements]);

    const sourceLabel = useMemo(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return 'Source';
        }

        const sourceValue = clusterElements['source'];

        if (!sourceValue || Array.isArray(sourceValue)) {
            return 'Source';
        }

        const sourceElement = sourceValue as unknown as NodeDataType;

        const typeSegments = sourceElement.type?.split('/') || [];

        const componentName = sourceElement.componentName || typeSegments[0] || '';

        const definition = componentDefinitions.find(
            (componentDefinition) => componentDefinition.name === componentName
        );

        return definition?.title || componentName || 'Source';
    }, [rootClusterElementNodeData?.clusterElements, componentDefinitions]);

    const destinationLabel = useMemo(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return 'Destination';
        }

        const destinationValue = clusterElements['destination'];

        if (!destinationValue || Array.isArray(destinationValue)) {
            return 'Destination';
        }

        const destinationElement = destinationValue as unknown as NodeDataType;

        const typeSegments = destinationElement.type?.split('/') || [];

        const componentName = destinationElement.componentName || typeSegments[0] || '';

        const definition = componentDefinitions.find(
            (componentDefinition) => componentDefinition.name === componentName
        );

        return definition?.title || componentName || 'Destination';
    }, [rootClusterElementNodeData?.clusterElements, componentDefinitions]);

    const displayConditionsQuery = useGetClusterElementParameterDisplayConditionsQuery(
        {
            clusterElementType: 'PROCESSOR',
            clusterElementWorkflowNodeName: processor?.name || '',
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
        },
        !!processor && !!workflow.id && !!rootClusterElementNodeData?.workflowNodeName
    );

    const setupNodeDetailsPanelForProcessor = useCallback(
        (processorItem: ProcessorItemI) => {
            const typeSegments = processorItem.type.split('/');
            const clusterElementName = typeSegments[2];
            const componentVersion = parseInt(typeSegments[1]?.replace(/^v/, '')) || 1;

            let processorMetadata: NodeDataType['metadata'] | undefined;
            let processorParameters: NodeDataType['parameters'] | undefined;

            if (rootClusterElementNodeData?.workflowNodeName && workflow.definition) {
                const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

                const mainClusterRootTask = getTask({
                    tasks: workflowDefinitionTasks,
                    workflowNodeName: rootClusterElementNodeData.workflowNodeName,
                });

                if (mainClusterRootTask?.clusterElements) {
                    const clusterElement = getClusterElementByName(
                        mainClusterRootTask.clusterElements,
                        processorItem.name
                    );

                    if (clusterElement) {
                        processorMetadata = clusterElement.metadata;
                        processorParameters = clusterElement.parameters;
                    }
                }
            }

            const processorNodeData: NodeDataType = {
                clusterElementName,
                clusterElementType: 'processor',
                componentName: processorItem.componentName,
                connections: [],
                description: '',
                label: processorItem.label,
                metadata: processorMetadata,
                name: processorItem.name,
                operationName: processorItem.operationName,
                parameters: processorParameters,
                parentClusterRootId: rootClusterElementNodeData?.name,
                type: processorItem.type,
                version: componentVersion,
                workflowNodeName: processorItem.name,
            };

            setCurrentNode(processorNodeData);

            setCurrentComponent((previousCurrentComponent) => ({
                ...processorNodeData,
                displayConditions: previousCurrentComponent?.displayConditions,
                workflowNodeName: processorItem.name,
            }));
        },
        [
            rootClusterElementNodeData?.name,
            rootClusterElementNodeData?.workflowNodeName,
            setCurrentComponent,
            setCurrentNode,
            workflow.definition,
        ]
    );

    const handleAutoMap = useCallback(async () => {
        if (!processor || !rootClusterElementNodeData?.workflowNodeName || !workflow.id) {
            return;
        }

        setAutoMapping(true);

        try {
            const optionApi = new WorkflowNodeOptionApi();

            const baseRequest = {
                clusterElementType: 'PROCESSOR',
                clusterElementWorkflowNodeName: processor.name,
                environmentId: currentEnvironmentId,
                id: workflow.id,
                workflowNodeName: rootClusterElementNodeData.workflowNodeName,
            };

            const [sourceOptions, destinationOptions] = await Promise.all([
                optionApi.getClusterElementNodeOptions({
                    ...baseRequest,
                    propertyName: 'mappings[0].sourceField',
                }),
                optionApi.getClusterElementNodeOptions({
                    ...baseRequest,
                    propertyName: 'mappings[0].destinationField',
                }),
            ]);

            const destinationValues = new Set(
                destinationOptions.map((option) => option.value as string).filter(Boolean)
            );

            const matchedMappings = sourceOptions
                .filter((sourceOption) => destinationValues.has(sourceOption.value as string))
                .map((sourceOption) => ({
                    destinationField: sourceOption.value as string,
                    sourceField: sourceOption.value as string,
                }));

            if (matchedMappings.length === 0) {
                return;
            }

            await new WorkflowNodeParameterApi().updateClusterElementParameter({
                clusterElementType: 'PROCESSOR',
                clusterElementWorkflowNodeName: processor.name,
                environmentId: currentEnvironmentId,
                id: workflow.id,
                updateClusterElementParameterRequest: {
                    path: 'mappings',
                    type: 'ARRAY',
                    value: matchedMappings as unknown as object,
                },
                workflowNodeName: rootClusterElementNodeData.workflowNodeName,
            });

            setCurrentComponent((previousCurrentComponent) => ({
                ...previousCurrentComponent!,
                parameters: {
                    ...previousCurrentComponent?.parameters,
                    mappings: matchedMappings,
                },
            }));

            setPropertiesKey((previousKey) => previousKey + 1);

            invalidateWorkflowQueries();
        } catch (error) {
            console.warn('Auto-map failed:', error);
        } finally {
            setAutoMapping(false);
        }
    }, [
        currentEnvironmentId,
        invalidateWorkflowQueries,
        processor,
        rootClusterElementNodeData?.workflowNodeName,
        setCurrentComponent,
        workflow.id,
    ]);

    useEffect(() => {
        if (!processor) {
            setProcessorProperties([]);

            return;
        }

        const typeSegments = processor.type.split('/');
        const clusterElementName = typeSegments[2];
        const componentVersion = parseInt(typeSegments[1]?.replace(/^v/, '')) || 1;

        const fetchProperties = async () => {
            try {
                const definition = await queryClient.fetchQuery({
                    queryFn: () =>
                        new ClusterElementDefinitionApi().getComponentClusterElementDefinition({
                            clusterElementName,
                            clusterElementType: 'PROCESSOR',
                            componentName: processor.componentName,
                            componentVersion: componentVersion,
                        }),
                    queryKey: ClusterElementDefinitionKeys.clusterElementDefinition({
                        clusterElementName,
                        clusterElementType: 'PROCESSOR',
                        componentName: processor.componentName,
                        componentVersion: componentVersion,
                    }),
                });

                setProcessorProperties((definition.properties as PropertyAllType[]) || []);
            } catch (error) {
                console.warn('Failed to fetch processor cluster element definition:', error);

                setProcessorProperties([]);
            }
        };

        fetchProperties();
        setupNodeDetailsPanelForProcessor(processor);
    }, [processor, queryClient, setupNodeDetailsPanelForProcessor]);

    return {
        autoMapping,
        destinationLabel,
        displayConditionsQuery,
        handleAutoMap,
        hasSourceAndDestination,
        processor,
        processorProperties,
        propertiesKey,
        sourceLabel,
    };
}
