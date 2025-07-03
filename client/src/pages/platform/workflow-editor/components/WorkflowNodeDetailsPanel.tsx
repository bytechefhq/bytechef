import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE, TASK_DISPATCHER_DATA_KEY_MAP} from '@/shared/constants';
import {
    ActionDefinition,
    ActionDefinitionApi,
    ClusterElementDefinition,
    ClusterElementDefinitionApi,
    ComponentConnection,
    ComponentDefinition,
    ComponentDefinitionBasic,
    GetComponentActionDefinitionRequest,
    GetComponentClusterElementDefinitionRequest,
    GetComponentTriggerDefinitionRequest,
    TaskDispatcherDefinition,
    TriggerDefinitionApi,
    WorkflowNodeOutput,
    WorkflowTask,
} from '@/shared/middleware/platform/configuration';
import {useDeleteWorkflowNodeTestOutputMutation} from '@/shared/mutations/platform/workflowNodeTestOutputs.mutations';
import {ActionDefinitionKeys} from '@/shared/queries/platform/actionDefinitions.queries';
import {
    ClusterElementDefinitionKeys,
    useGetClusterElementDefinitionQuery,
} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {
    TriggerDefinitionKeys,
    useGetTriggerDefinitionQuery,
} from '@/shared/queries/platform/triggerDefinitions.queries';
import {WorkflowNodeDynamicPropertyKeys} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useGetWorkflowNodeParameterDisplayConditionsQuery} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {
    BranchCaseType,
    ComponentPropertiesType,
    NodeDataType,
    PropertyAllType,
    TabNameType,
    UpdateWorkflowMutationType,
    WorkflowNodeType,
} from '@/shared/types';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {useQueryClient} from '@tanstack/react-query';
import {InfoIcon, XIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import isEqual from 'react-fast-compare';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import {getClusterElementsLabel} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getDataPillsFromProperties from '../utils/getDataPillsFromProperties';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import saveClusterElementFieldChange from '../utils/saveClusterElementFieldChange';
import saveClusterElementNodesPosition from '../utils/saveClusterElementNodesPosition';
import saveTaskDispatcherSubtaskFieldChange from '../utils/saveTaskDispatcherSubtaskFieldChange';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import CurrentOperationSelect from './CurrentOperationSelect';
import DescriptionTab from './node-details-tabs/DescriptionTab';
import ConnectionTab from './node-details-tabs/connection-tab/ConnectionTab';
import OutputTab from './node-details-tabs/output-tab/OutputTab';

const TABS: Array<{label: string; name: TabNameType}> = [
    {
        label: 'Description',
        name: 'description',
    },
    {
        label: 'Connection',
        name: 'connection',
    },
    {
        label: 'Properties',
        name: 'properties',
    },
    {
        label: 'Output',
        name: 'output',
    },
];

interface WorkflowNodeDetailsPanelProps {
    className?: string;
    invalidateWorkflowQueries: () => void;
    previousComponentDefinitions: Array<ComponentDefinitionBasic>;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowNodeOutputs: WorkflowNodeOutput[];
}

const WorkflowNodeDetailsPanel = ({
    className,
    invalidateWorkflowQueries,
    previousComponentDefinitions,
    updateWorkflowMutation,
    workflowNodeOutputs,
}: WorkflowNodeDetailsPanelProps) => {
    const [currentNodeName, setCurrentNodeName] = useState<string | undefined>();
    const [currentOperationName, setCurrentOperationName] = useState('');
    const [currentOperationProperties, setCurrentOperationProperties] = useState<Array<PropertyAllType>>([]);
    const [currentActionDefinition, setCurrentActionDefinition] = useState<
        ActionDefinition | ClusterElementDefinition | undefined
    >();
    const [currentActionFetched, setCurrentActionFetched] = useState(false);
    const [currentClusterElementName, setCurrentClusterElementName] = useState<string | undefined>();
    const [clusterElementComponentOperations, setClusterElementComponentOperations] = useState<Array<WorkflowNodeType>>(
        []
    );

    const isOperationNameUpdatingRef = useRef(false);

    const {
        activeTab,
        currentComponent,
        currentNode,
        setActiveTab,
        setCurrentComponent,
        setCurrentNode,
        workflowNodeDetailsPanelOpen,
    } = useWorkflowNodeDetailsPanelStore();

    const {setDataPills, workflow, workflowNodes} = useWorkflowDataStore();

    const {clusterElementsCanvasOpen, rootClusterElementNodeData} = useWorkflowEditorStore();

    const queryClient = useQueryClient();

    const isClusterElement = !!currentNode?.clusterElementType;

    const {data: currentComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: currentNode?.componentName || '',
            componentVersion: currentNode?.version || 1,
        },
        !!currentNode && !currentNode.taskDispatcher
    );

    const {data: workflowTestConfigurationConnections} = useGetWorkflowTestConfigurationConnectionsQuery(
        {
            workflowId: workflow.id as string,
            workflowNodeName: currentNode?.workflowNodeName as string,
        },
        !!workflow.id && !!currentNode
    );

    const {data: currentClusterElementDefinition} = useGetClusterElementDefinitionQuery(
        {
            clusterElementName: currentNode?.clusterElementName as string,
            componentName: currentNode?.componentName as string,
            componentVersion: currentNode?.version as number,
        },
        isClusterElement
    );

    const deleteWorkflowNodeTestOutputMutation = useDeleteWorkflowNodeTestOutputMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: [...WorkflowNodeOutputKeys.workflowNodeOutputs, workflow.id],
            });
        },
    });

    const {nodeNames} = workflow;

    const matchingOperation = useMemo(
        () =>
            [
                ...(currentComponentDefinition?.actions || []),
                ...(currentComponentDefinition?.triggers || []),
                ...(currentComponentDefinition?.clusterElements || []),
            ].find((action) => action?.name === currentOperationName),
        [currentComponentDefinition, currentOperationName]
    );

    const getTriggerName = useCallback((): string => {
        const currentComponentTriggerNames = currentComponentDefinition?.triggers?.map((trigger) => trigger.name);

        return currentComponentTriggerNames?.includes(currentOperationName)
            ? currentOperationName
            : (currentComponentDefinition?.triggers?.[0]?.name as string);
    }, [currentComponentDefinition, currentOperationName]);

    const {data: currentTriggerDefinition, isFetched: currentTriggerFetched} = useGetTriggerDefinitionQuery(
        {
            componentName: currentComponentDefinition?.name as string,
            componentVersion: currentComponentDefinition?.version as number,
            triggerName: getTriggerName(),
        },
        !!currentNode?.componentName && currentNode?.trigger && !!currentComponentDefinition
    );

    const {data: currentTaskDispatcherDefinition} = useGetTaskDispatcherDefinitionQuery(
        {
            taskDispatcherName: currentNode?.componentName || '',
            taskDispatcherVersion: currentNode?.version || 1,
        },
        !!currentNode && !!currentNode.taskDispatcher
    );

    const displayConditionsQuery = useGetWorkflowNodeParameterDisplayConditionsQuery(
        {
            id: workflow.id!,
            workflowNodeName: currentNodeName!,
        },
        !!currentNodeName && currentNodeName !== 'manual' && currentNodeName !== currentClusterElementName
    );

    const {data: workflowNodeParameterDisplayConditions} = displayConditionsQuery;

    const currentNodeDefinition = useMemo(() => {
        if (currentNode?.trigger) {
            return currentTriggerDefinition;
        }

        if (currentNode?.taskDispatcher) {
            return currentTaskDispatcherDefinition;
        }

        if (clusterElementsCanvasOpen && currentNode?.clusterElementType) {
            return currentClusterElementDefinition;
        }

        return currentActionDefinition;
    }, [
        clusterElementsCanvasOpen,
        currentNode,
        currentTriggerDefinition,
        currentTaskDispatcherDefinition,
        currentActionDefinition,
        currentClusterElementDefinition,
    ]);

    const currentNodeIndex = useMemo(
        () => currentNode && nodeNames?.indexOf(currentNode?.workflowNodeName),
        [currentNode, nodeNames]
    );

    const previousNodeNames = useMemo(
        () => (nodeNames.length > 1 ? nodeNames?.slice(0, currentNodeIndex) : []),
        [nodeNames, currentNodeIndex]
    );

    const previousComponentProperties: Array<ComponentPropertiesType> = useMemo(
        () =>
            previousComponentDefinitions?.map((componentDefinition, index) => {
                const outputSchemaDefinition: PropertyAllType | undefined =
                    workflowNodeOutputs[index]?.outputResponse?.outputSchema;

                const properties = outputSchemaDefinition?.properties?.length
                    ? outputSchemaDefinition.properties
                    : outputSchemaDefinition?.items;

                return {
                    componentDefinition,
                    properties,
                };
            }),
        [previousComponentDefinitions, workflowNodeOutputs]
    );

    const hasOutputData = useMemo(
        () =>
            currentNodeDefinition?.outputDefined ||
            (currentNodeDefinition as TaskDispatcherDefinition)?.variablePropertiesDefined,
        [currentNodeDefinition]
    );

    const currentWorkflowTrigger = useMemo(
        () => workflow.triggers?.find((trigger) => trigger.name === currentNode?.workflowNodeName),
        [workflow.triggers, currentNode]
    );

    const currentWorkflowTask = useMemo(
        () => workflow.tasks?.find((task) => task.name === currentNode?.workflowNodeName),
        [workflow.tasks, currentNode]
    );

    const currentWorkflowNodeConnections: ComponentConnection[] = useMemo(
        () => currentWorkflowTask?.connections || currentWorkflowTrigger?.connections || [],
        [currentWorkflowTask, currentWorkflowTrigger]
    );

    const nodeTabs = useMemo(() => {
        return TABS.filter(({name}) => {
            if (name === 'connection') {
                return currentWorkflowNodeConnections.length > 0;
            }

            // if (name === 'clusterElements') {
            //     return currentComponentDefinition?.clusterRoot;
            // }

            if (name === 'output') {
                return hasOutputData;
            }

            if (name === 'properties') {
                return currentNode?.taskDispatcher
                    ? currentTaskDispatcherDefinition?.properties?.length
                    : currentOperationProperties?.length;
            }

            return true;
        });
    }, [
        currentWorkflowNodeConnections,
        hasOutputData,
        currentNode,
        currentTaskDispatcherDefinition,
        currentOperationProperties,
    ]);

    const currentWorkflowNode = useMemo(
        () => currentComponentDefinition || currentTaskDispatcherDefinition || currentClusterElementDefinition,
        [currentClusterElementDefinition, currentComponentDefinition, currentTaskDispatcherDefinition]
    );

    const currentOperationFetched = useMemo(
        () => currentActionFetched || currentTriggerFetched,
        [currentActionFetched, currentTriggerFetched]
    );

    const operationDataMissing = useMemo(
        () => currentComponent?.operationName && (!matchingOperation?.name || !currentOperationFetched),
        [currentComponent, matchingOperation, currentOperationFetched]
    );

    const tabDataExists = useMemo(
        () =>
            (!currentNode?.trigger && !currentNode?.taskDispatcher && currentActionFetched) ||
            currentNode?.taskDispatcher ||
            (currentNode?.trigger &&
                currentTriggerFetched &&
                nodeTabs.length > 1 &&
                currentNode.componentName !== 'manual'),
        [currentNode, currentActionFetched, currentTriggerFetched, nodeTabs]
    );

    const nodeDefinition = useMemo(
        () =>
            currentComponentDefinition ||
            currentTaskDispatcherDefinition ||
            currentTriggerDefinition ||
            currentClusterElementDefinition,
        [
            currentClusterElementDefinition,
            currentComponentDefinition,
            currentTaskDispatcherDefinition,
            currentTriggerDefinition,
        ]
    );

    const currentWorkflowNodeOperations = useMemo(() => {
        if (clusterElementsCanvasOpen && isClusterElement) {
            return (currentWorkflowNode as ComponentDefinition)?.clusterElements;
        }

        return (
            (currentWorkflowNode as ComponentDefinition)?.actions ??
            (currentWorkflowNode as ComponentDefinition)?.triggers
        );
    }, [clusterElementsCanvasOpen, currentWorkflowNode, isClusterElement]);

    const handleOperationSelectChange = useCallback(
        async (newOperationName: string) => {
            setCurrentOperationName(newOperationName);

            if (!currentComponentDefinition || !currentComponent) {
                return;
            }

            await deleteWorkflowNodeTestOutputMutation.mutateAsync({
                id: workflow.id!,
                workflowNodeName: currentNode!.name,
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties,
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowNodeOptionKeys.workflowNodeOptions,
            });

            let operationData;

            if (currentNode?.trigger) {
                const triggerDefinitionRequest: GetComponentTriggerDefinitionRequest = {
                    componentName: currentComponentDefinition?.name,
                    componentVersion: currentComponentDefinition?.version,
                    triggerName: newOperationName,
                };

                operationData = await queryClient.fetchQuery({
                    queryFn: () => new TriggerDefinitionApi().getComponentTriggerDefinition(triggerDefinitionRequest),
                    queryKey: TriggerDefinitionKeys.triggerDefinition(triggerDefinitionRequest),
                });
            } else if (clusterElementsCanvasOpen && isClusterElement) {
                const clusterElementDefinitionRequest: GetComponentClusterElementDefinitionRequest = {
                    clusterElementName: newOperationName,
                    componentName: currentComponentDefinition?.name,
                    componentVersion: currentComponentDefinition?.version,
                };

                operationData = await queryClient.fetchQuery({
                    queryFn: () =>
                        new ClusterElementDefinitionApi().getComponentClusterElementDefinition(
                            clusterElementDefinitionRequest
                        ),
                    queryKey: ClusterElementDefinitionKeys.clusterElementDefinition(clusterElementDefinitionRequest),
                });
            } else {
                const componentActionDefinitionRequest: GetComponentActionDefinitionRequest = {
                    actionName: newOperationName,
                    componentName: currentComponentDefinition.name,
                    componentVersion: currentComponentDefinition.version,
                };

                operationData = await queryClient.fetchQuery({
                    queryFn: () =>
                        new ActionDefinitionApi().getComponentActionDefinition(componentActionDefinitionRequest),
                    queryKey: ActionDefinitionKeys.actionDefinition(componentActionDefinitionRequest),
                });
            }

            const {componentName, description, label, workflowNodeName} = currentComponent;

            const nodeData: NodeDataType = {
                componentName,
                description,
                label,
                name: workflowNodeName || currentNode?.workflowNodeName || '',
                operationName: newOperationName,
                parameters: getParametersWithDefaultValues({
                    properties: operationData.properties as Array<PropertyAllType>,
                }),
                trigger: currentNode?.trigger,
                type: `${componentName}/v${currentComponentDefinition.version}/${newOperationName}`,
                workflowNodeName,
            };

            const isTaskDispatcherSubtask = Object.values(TASK_DISPATCHER_DATA_KEY_MAP).some(
                (dataKey) => dataKey && currentNode?.[dataKey as keyof typeof currentNode]
            );

            if (isTaskDispatcherSubtask) {
                saveTaskDispatcherSubtaskFieldChange({
                    currentComponentDefinition,
                    currentNodeIndex: currentNodeIndex ?? 0,
                    currentOperationProperties,
                    fieldUpdate: {
                        field: 'operation',
                        value: newOperationName,
                    },
                    invalidateWorkflowQueries,
                    queryClient,
                    updateWorkflowMutation,
                });

                return;
            }

            if (isClusterElement) {
                saveClusterElementFieldChange({
                    currentClusterElementName: currentNode.name,
                    currentComponentDefinition,
                    currentOperationProperties,
                    fieldUpdate: {
                        field: 'operation',
                        value: newOperationName,
                    },
                    invalidateWorkflowQueries,
                    queryClient,
                    updateWorkflowMutation,
                });

                return;
            }

            saveWorkflowDefinition({
                invalidateWorkflowQueries,
                nodeData,
                onSuccess: () => {
                    setCurrentComponent({
                        ...currentComponent,
                        displayConditions: {},
                        metadata: {},
                        operationName: newOperationName,
                        parameters: getParametersWithDefaultValues({
                            properties: currentOperationProperties as Array<PropertyAllType>,
                        }),
                        type: `${componentName}/v${currentComponentDefinition.version}/${newOperationName}`,
                    });

                    setCurrentNode({
                        ...currentNode,
                        componentName,
                        displayConditions: {},
                        metadata: {},
                        name: workflowNodeName || currentNode?.workflowNodeName || '',
                        operationName: newOperationName,
                        parameters: getParametersWithDefaultValues({
                            properties: currentOperationProperties as Array<PropertyAllType>,
                        }),
                        type: `${componentName}/v${currentComponentDefinition.version}/${newOperationName}`,
                        workflowNodeName,
                    });
                },
                updateWorkflowMutation,
            });
        },
        [
            currentComponentDefinition,
            currentComponent,
            queryClient,
            currentNode,
            deleteWorkflowNodeTestOutputMutation,
            invalidateWorkflowQueries,
            clusterElementsCanvasOpen,
            isClusterElement,
            updateWorkflowMutation,
            currentNodeIndex,
            currentOperationProperties,
            setCurrentComponent,
            setCurrentNode,
            workflow.id,
        ]
    );

    const handlePanelClose = useCallback(() => {
        if (clusterElementsCanvasOpen) {
            saveClusterElementNodesPosition({
                invalidateWorkflowQueries,
                queryClient,
                updateWorkflowMutation,
                workflow,
            });
        }

        useWorkflowNodeDetailsPanelStore.getState().reset();
    }, [clusterElementsCanvasOpen, invalidateWorkflowQueries, queryClient, updateWorkflowMutation, workflow]);

    // Get the node version for different definition types
    function getNodeVersion(node: typeof currentWorkflowNode): string {
        if (!node) {
            return '1';
        }

        if ('version' in node) {
            return node.version.toString();
        }

        if ('componentVersion' in node) {
            return node.componentVersion.toString();
        }

        return '1';
    }

    // Set current node name
    useEffect(() => {
        if (currentNode?.name) {
            setCurrentNodeName(currentNode.name);

            if (isClusterElement) {
                setCurrentClusterElementName(currentNode.name);
            } else {
                setCurrentClusterElementName(undefined);
            }
        } else {
            setCurrentNodeName(undefined);
        }
    }, [currentNode?.name, isClusterElement]);

    // Set currentOperationProperties depending if the current node is a trigger or an action
    useEffect(() => {
        if (currentNodeDefinition?.properties) {
            setCurrentOperationProperties(currentNodeDefinition?.properties);
        }
    }, [currentNodeDefinition?.properties]);

    // Set availableDataPills depending on previousComponentProperties
    useEffect(() => {
        if (!previousComponentProperties) {
            return;
        }

        let filteredNodeNames = previousNodeNames;

        if (currentNode?.conditionData) {
            const parentConditionTask = workflow.tasks?.find(
                (task) => task.name === currentNode.conditionData?.conditionId
            );

            if (!parentConditionTask) {
                return;
            }

            const {conditionCase} = currentNode.conditionData;

            const oppositeConditionCase =
                conditionCase === CONDITION_CASE_TRUE ? CONDITION_CASE_FALSE : CONDITION_CASE_TRUE;

            const oppositeConditionCaseNodeNames = parentConditionTask.parameters?.[oppositeConditionCase].map(
                (task: WorkflowTask) => task.name
            );

            filteredNodeNames = filteredNodeNames.filter(
                (nodeName) => !oppositeConditionCaseNodeNames?.includes(nodeName)
            );
        } else if (currentNode?.branchData) {
            const parentBranchTask = workflow.tasks?.find((task) => task.name === currentNode.branchData?.branchId);

            if (!parentBranchTask || !parentBranchTask.parameters) {
                return;
            }

            const {caseKey} = currentNode.branchData;
            const branchCases: BranchCaseType[] = [
                {key: 'default', tasks: parentBranchTask.parameters.default},
                ...parentBranchTask.parameters.cases,
            ];

            let otherCaseKeys;

            if (caseKey === 'default') {
                otherCaseKeys = branchCases.map((caseItem) => caseItem.key);
            } else {
                otherCaseKeys = ['default'];

                branchCases.forEach((caseItem) => {
                    if (caseItem.key !== caseKey) {
                        otherCaseKeys.push(caseItem.key);
                    }
                });
            }

            const otherCasesNodeNames = branchCases
                .filter((caseItem) => otherCaseKeys.includes(caseItem.key))
                .map((caseItem) => caseItem.tasks.map((task: WorkflowTask) => task.name))
                .flat(Infinity);

            filteredNodeNames = filteredNodeNames.filter((nodeName) => !otherCasesNodeNames?.includes(nodeName));
        }

        const dataPills = getDataPillsFromProperties(previousComponentProperties!, filteredNodeNames);

        setDataPills(dataPills.flat(Infinity));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [previousComponentProperties.length, previousNodeNames]);

    // Tab switching logic
    useEffect(() => {
        if (activeTab === 'connection' && currentWorkflowNodeConnections.length === 0) {
            setActiveTab('description');
        }

        if (currentComponentDefinition?.name === 'manual') {
            setActiveTab('description');
        }

        if (
            activeTab === 'properties' &&
            ((!currentNode?.trigger && currentActionFetched) || (currentNode?.trigger && currentTriggerFetched)) &&
            !currentOperationProperties
        ) {
            setActiveTab('description');
        }

        if (activeTab === 'output' && !hasOutputData) {
            setActiveTab('description');
        }

        if (activeTab === 'properties' && !operationDataMissing && !currentOperationProperties?.length) {
            setActiveTab('description');
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        activeTab,
        currentActionDefinition?.outputDefined,
        currentActionFetched,
        currentOperationProperties?.length,
        currentComponentDefinition?.name,
    ]);

    // If the current component requires a connection, set the active tab to 'connection'
    useEffect(() => {
        if (currentComponentDefinition?.connectionRequired && !workflowTestConfigurationConnections?.length) {
            setActiveTab('connection');
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentComponentDefinition]);

    // Update currentNode with connection data, operationName and type
    useEffect(() => {
        if (!currentNode) {
            return;
        }

        let updatedNode = {...currentNode};

        if (currentNode.operationName && currentOperationName) {
            updatedNode = {
                ...updatedNode,
                operationName: currentOperationName,
                triggerType: currentTriggerDefinition?.type,
                type: `${currentComponent?.componentName}/v${currentComponentDefinition?.version}/${currentOperationName}`,
            };
        }

        if (currentWorkflowNodeConnections.length) {
            updatedNode = {
                ...updatedNode,
                connectionId: workflowTestConfigurationConnections
                    ? workflowTestConfigurationConnections[0]?.connectionId
                    : undefined,
                connections: currentWorkflowNodeConnections,
            };
        }

        if (!isEqual(updatedNode, currentNode)) {
            setCurrentNode(updatedNode);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        currentNode,
        currentOperationName,
        currentComponent?.componentName,
        currentComponentDefinition?.version,
        currentWorkflowNodeConnections,
        workflowTestConfigurationConnections,
        currentTriggerDefinition,
    ]);

    // Find cluster element component operations
    useEffect(() => {
        if (!clusterElementsCanvasOpen) {
            return;
        }

        const currentRootClusterTask = workflow?.tasks?.find(
            (task) => task.name === rootClusterElementNodeData?.workflowNodeName
        );

        const currentRootClusterTaskClusterElements = currentRootClusterTask?.clusterElements;

        if (currentRootClusterTaskClusterElements) {
            let clusterElementsWorkflowNodeTypes: WorkflowNodeType[] = [];

            Object.entries(currentRootClusterTaskClusterElements).forEach(([, value]) => {
                if (Array.isArray(value)) {
                    const multipleClusterElements = value.map((element) => ({
                        name: element.componentName || '',
                        operationName: element.type ? element.type.split('/')[2] : '',
                        version: +element.type!.split('/')[1].replace('v', ''),
                        workflowNodeName: element.name || '',
                    }));

                    clusterElementsWorkflowNodeTypes = [
                        ...clusterElementsWorkflowNodeTypes,
                        ...multipleClusterElements,
                    ];
                } else {
                    clusterElementsWorkflowNodeTypes.push({
                        name: value.componentName || '',
                        operationName: value.type ? value.type.split('/')[2] : '',
                        version: +value.type!.split('/')[1].replace('v', ''),
                        workflowNodeName: value.name || '',
                    });
                }
            });

            if (clusterElementsWorkflowNodeTypes.length > 0) {
                setClusterElementComponentOperations(clusterElementsWorkflowNodeTypes);
            }
        }
    }, [clusterElementsCanvasOpen, rootClusterElementNodeData, workflow]);

    // Set currentOperationName depending on the currentWorkflowNode.operationName
    useEffect(() => {
        if (!workflowNodes?.length) {
            return;
        }

        let currentWorkflowNode;

        if (workflowNodes.length && !clusterElementsCanvasOpen && !isClusterElement) {
            currentWorkflowNode = workflowNodes.find(
                (workflowNode) => workflowNode.workflowNodeName === currentNode?.workflowNodeName
            );
        } else if (clusterElementsCanvasOpen) {
            if (currentNode?.rootClusterElement) {
                currentWorkflowNode = workflowNodes.find(
                    (action) => action.workflowNodeName === currentNode?.workflowNodeName
                );
            } else if (clusterElementComponentOperations) {
                currentWorkflowNode = clusterElementComponentOperations.find(
                    (action) => action.workflowNodeName === currentNode?.workflowNodeName
                );
            }
        }

        if (currentWorkflowNode?.operationName && currentWorkflowNode.operationName !== currentOperationName) {
            setCurrentOperationName(currentWorkflowNode.operationName);
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [clusterElementComponentOperations, currentNode?.workflowNodeName, currentOperationName, workflowNodes]);

    // Update display conditions when currentNode changes
    useEffect(() => {
        if (currentNode && workflowNodeParameterDisplayConditions?.displayConditions) {
            setCurrentNode({
                ...currentNode,
                displayConditions: workflowNodeParameterDisplayConditions.displayConditions,
            });
        }

        if (currentComponent && workflowNodeParameterDisplayConditions?.displayConditions) {
            if (currentComponent.workflowNodeName === currentNode?.name) {
                setCurrentComponent({
                    ...currentComponent,
                    displayConditions: workflowNodeParameterDisplayConditions.displayConditions,
                });
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowNodeParameterDisplayConditions?.displayConditions, currentNode?.name]);

    // Fetch current action definition when operation changes
    useEffect(() => {
        if (!currentOperationName || !currentComponentDefinition) {
            setCurrentActionDefinition(undefined);

            return;
        }

        if (isOperationNameUpdatingRef.current && currentOperationName !== currentNode?.operationName) {
            isOperationNameUpdatingRef.current = false;

            return;
        }

        if (clusterElementsCanvasOpen && currentComponentDefinition?.clusterElement) {
            const fetchClusterElementDefinition = async () => {
                const clusterElementDefinitionRequest: GetComponentClusterElementDefinitionRequest = {
                    clusterElementName: currentOperationName,
                    componentName: currentComponentDefinition?.name as string,
                    componentVersion: currentComponentDefinition?.version as number,
                };

                const clusterElementDefinition = await queryClient.fetchQuery({
                    queryFn: () =>
                        new ClusterElementDefinitionApi().getComponentClusterElementDefinition(
                            clusterElementDefinitionRequest
                        ),
                    queryKey: ClusterElementDefinitionKeys.clusterElementDefinition(clusterElementDefinitionRequest),
                });

                if (clusterElementDefinition) {
                    setCurrentActionDefinition(clusterElementDefinition);

                    setCurrentActionFetched(true);
                }
            };

            if (!!currentComponentDefinition.clusterElements && !!matchingOperation) {
                fetchClusterElementDefinition();
            } else {
                setCurrentActionDefinition(undefined);
            }
        } else {
            const fetchActionDefinition = async () => {
                const actionDefinitionRequest: GetComponentActionDefinitionRequest = {
                    actionName: currentOperationName ?? currentNode?.operationName,
                    componentName: currentComponentDefinition?.name as string,
                    componentVersion: currentComponentDefinition?.version as number,
                };

                const actionDefinition = await queryClient.fetchQuery({
                    queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(actionDefinitionRequest),
                    queryKey: ActionDefinitionKeys.actionDefinition(actionDefinitionRequest),
                });

                if (actionDefinition) {
                    setCurrentActionDefinition(actionDefinition);

                    setCurrentActionFetched(true);
                }
            };

            if (!!currentComponentDefinition?.actions && !currentNode?.trigger && !!matchingOperation) {
                fetchActionDefinition();
            } else {
                setCurrentActionDefinition(undefined);
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        currentComponentDefinition,
        currentNodeName,
        currentOperationName,
        matchingOperation,
        queryClient,
        clusterElementsCanvasOpen,
    ]);

    if (!workflowNodeDetailsPanelOpen) {
        return <></>;
    }

    return (
        <div
            className={twMerge(
                'absolute bottom-6 right-[69px] top-2 z-10 w-screen max-w-workflow-node-details-panel-width overflow-hidden rounded-md border border-stroke-neutral-secondary bg-background',
                className
            )}
            key={currentNode?.workflowNodeName}
        >
            {currentNode?.workflowNodeName && currentWorkflowNode && (
                <div className="flex h-full flex-col divide-y divide-muted bg-background">
                    <header className="flex items-center p-4 text-lg font-medium">
                        {currentWorkflowNode.icon && (
                            <InlineSVG
                                className="mr-2 size-6"
                                loader={<LoadingIcon className="ml-0 mr-2 size-6 text-muted-foreground" />}
                                src={currentWorkflowNode.icon}
                            />
                        )}

                        {currentNode?.label}

                        <span className="mx-2 text-sm text-muted-foreground">({currentNode?.workflowNodeName})</span>

                        {currentWorkflowNode.description && (
                            <Tooltip delayDuration={500}>
                                <TooltipTrigger>
                                    <InfoIcon className="size-4" />
                                </TooltipTrigger>

                                <TooltipPortal>
                                    <TooltipContent className="max-w-md" side="bottom">
                                        {currentComponentDefinition
                                            ? currentComponentDefinition.description
                                            : currentTaskDispatcherDefinition?.description}
                                    </TooltipContent>
                                </TooltipPortal>
                            </Tooltip>
                        )}

                        <button
                            aria-label="Close the node details dialog"
                            className="ml-auto pr-0"
                            onClick={handlePanelClose}
                        >
                            <XIcon aria-hidden="true" className="size-4 cursor-pointer" />
                        </button>
                    </header>

                    <main className="flex h-full flex-col overflow-hidden">
                        {!!currentWorkflowNodeOperations?.length && operationDataMissing && (
                            <div className="flex flex-col border-b border-muted p-4">
                                <span className="text-sm leading-6">Actions</span>

                                <Skeleton className="h-9 w-full" />
                            </div>
                        )}

                        {currentWorkflowNodeOperations && !operationDataMissing && (
                            <CurrentOperationSelect
                                clusterElementLabel={
                                    currentNode.clusterElementType &&
                                    getClusterElementsLabel(currentNode.clusterElementType)
                                }
                                description={
                                    currentNode?.trigger
                                        ? currentTriggerDefinition?.description
                                        : clusterElementsCanvasOpen && currentComponentDefinition?.clusterElement
                                          ? currentComponentDefinition?.description
                                          : currentActionDefinition?.description
                                }
                                handleValueChange={handleOperationSelectChange}
                                operations={
                                    (currentNode?.trigger
                                        ? currentComponentDefinition?.triggers
                                        : clusterElementsCanvasOpen && currentComponentDefinition?.clusterElement
                                          ? currentComponentDefinition?.clusterElements
                                          : currentComponentDefinition?.actions)!
                                }
                                triggerSelect={currentNode?.trigger}
                                value={currentOperationName}
                            />
                        )}

                        {tabDataExists && (
                            <div className="flex justify-center">
                                {nodeTabs.map((tab) => (
                                    <Button
                                        className={twMerge(
                                            'grow justify-center whitespace-nowrap rounded-none border-0 border-b border-border bg-background py-5 text-sm font-medium text-muted-foreground hover:border-blue-500 hover:text-blue-500 focus:border-blue-500 focus:text-blue-500 focus:outline-none',
                                            activeTab === tab?.name &&
                                                'border-blue-500 text-blue-500 hover:text-blue-500'
                                        )}
                                        key={tab.name}
                                        name={tab.name}
                                        onClick={() => setActiveTab(tab.name)}
                                        variant="ghost"
                                    >
                                        {tab.label}
                                    </Button>
                                ))}
                            </div>
                        )}

                        {currentNode.componentName !== 'manual' && !tabDataExists && (
                            <div className="flex justify-center space-x-2 border-b border-border p-2">
                                <Skeleton className="h-6 w-1/4" />

                                <Skeleton className="h-6 w-1/4" />

                                <Skeleton className="h-6 w-1/4" />

                                <Skeleton className="h-6 w-1/4" />
                            </div>
                        )}

                        <ScrollArea className="h-full max-w-workflow-node-details-panel-width">
                            <div className="size-full max-w-workflow-node-details-panel-width">
                                {activeTab === 'description' &&
                                    (nodeDefinition ? (
                                        <DescriptionTab
                                            invalidateWorkflowQueries={invalidateWorkflowQueries}
                                            key={`${currentNode?.componentName}-${currentNode?.type}_description`}
                                            nodeDefinition={nodeDefinition}
                                            updateWorkflowMutation={updateWorkflowMutation}
                                        />
                                    ) : (
                                        <div className="flex flex-col gap-y-4 p-4">
                                            <div className="flex flex-col gap-y-2">
                                                <Skeleton className="h-6 w-1/4" />

                                                <Skeleton className="h-8 w-full" />
                                            </div>

                                            <div className="flex flex-col gap-y-2">
                                                <Skeleton className="h-6 w-1/4" />

                                                <Skeleton className="h-24 w-full" />
                                            </div>
                                        </div>
                                    ))}

                                {activeTab === 'connection' &&
                                    currentWorkflowNodeConnections.length > 0 &&
                                    currentNode &&
                                    currentComponentDefinition && (
                                        <ConnectionTab
                                            componentConnections={currentWorkflowNodeConnections}
                                            key={`${currentNode?.componentName}-${currentNode?.type}_connection`}
                                            workflowId={workflow.id!}
                                            workflowNodeName={currentNode?.workflowNodeName}
                                            workflowTestConfigurationConnections={workflowTestConfigurationConnections}
                                        />
                                    )}

                                {activeTab === 'properties' &&
                                    (!operationDataMissing && currentOperationProperties?.length ? (
                                        <Properties
                                            customClassName="p-4"
                                            displayConditionsQuery={displayConditionsQuery}
                                            key={`${currentNode?.componentName}-${currentNode?.type}_${currentOperationName}_properties`}
                                            operationName={currentOperationName}
                                            properties={currentOperationProperties}
                                        />
                                    ) : (
                                        <div className="flex size-full items-center justify-center">
                                            <LoadingIcon /> Loading...
                                        </div>
                                    ))}

                                {activeTab === 'output' && (
                                    <OutputTab
                                        connectionMissing={
                                            (currentComponentDefinition?.connectionRequired ?? false) &&
                                            !workflowTestConfigurationConnections?.length
                                        }
                                        currentNode={currentNode}
                                        key={`${currentNode?.componentName}-${currentNode?.type}_output`}
                                        variablePropertiesDefined={
                                            currentTaskDispatcherDefinition?.variablePropertiesDefined
                                        }
                                        workflowId={workflow.id!}
                                    />
                                )}
                            </div>
                        </ScrollArea>
                    </main>

                    <footer className="z-50 mt-auto flex bg-background px-4 py-2">
                        <Select defaultValue={getNodeVersion(currentWorkflowNode)}>
                            <SelectTrigger className="w-auto border-none shadow-none">
                                <SelectValue placeholder="Choose version..." />
                            </SelectTrigger>

                            <SelectContent>
                                <SelectItem value="1">v1</SelectItem>
                            </SelectContent>
                        </Select>
                    </footer>
                </div>
            )}
        </div>
    );
};

export default WorkflowNodeDetailsPanel;
