import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import CurrentOperationSelect from '@/pages/platform/workflow-editor/components/CurrentOperationSelect';
import DescriptionTab from '@/pages/platform/workflow-editor/components/node-details-tabs/DescriptionTab';
import ConnectionTab from '@/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTab';
import OutputTab from '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTab';
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
    TriggerDefinition,
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
import {
    ClusterElementDynamicPropertyKeys,
    WorkflowNodeDynamicPropertyKeys,
} from '@/shared/queries/platform/workflowNodeDynamicProperties.queries';
import {WorkflowNodeOptionKeys} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {
    useGetClusterElementParameterDisplayConditionsQuery,
    useGetWorkflowNodeParameterDisplayConditionsQuery,
} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {
    BranchCaseType,
    ComponentPropertiesType,
    DataPillType,
    NodeDataType,
    PropertyAllType,
    TabNameType,
    UpdateWorkflowMutationType,
    WorkflowNodeType,
} from '@/shared/types';
import {TooltipPortal} from '@radix-ui/react-tooltip';
import {useQueryClient} from '@tanstack/react-query';
import {InfoIcon, XIcon} from 'lucide-react';
import {ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import isEqual from 'react-fast-compare';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/shallow';

import {
    convertNameToSnakeCase,
    extractClusterElementComponentOperations,
    getClusterElementsLabel,
} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getDataPillsFromProperties from '../utils/getDataPillsFromProperties';
import getParametersWithDefaultValues from '../utils/getParametersWithDefaultValues';
import saveClusterElementFieldChange from '../utils/saveClusterElementFieldChange';
import saveTaskDispatcherSubtaskFieldChange from '../utils/saveTaskDispatcherSubtaskFieldChange';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import {getTaskDispatcherTask} from '../utils/taskDispatcherConfig';
import {DescriptionTabSkeleton, FieldsetSkeleton, PropertiesTabSkeleton} from './WorkflowEditorSkeletons';

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
    closeButton?: ReactNode;
    invalidateWorkflowQueries: () => void;
    previousComponentDefinitions: Array<ComponentDefinitionBasic>;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowNodeOutputs: WorkflowNodeOutput[];
}

const WorkflowNodeDetailsPanel = ({
    className,
    closeButton,
    invalidateWorkflowQueries,
    previousComponentDefinitions,
    updateWorkflowMutation,
    workflowNodeOutputs,
}: WorkflowNodeDetailsPanelProps) => {
    const [currentNodeName, setCurrentNodeName] = useState<string | undefined>();
    const [currentOperationName, setCurrentOperationName] = useState('');
    const [currentOperationProperties, setCurrentOperationProperties] = useState<Array<PropertyAllType>>([]);
    const [currentActionDefinition, setCurrentActionDefinition] = useState<
        ActionDefinition | ClusterElementDefinition | TriggerDefinition | undefined
    >();
    const [currentActionFetched, setCurrentActionFetched] = useState(false);
    const [currentClusterElementName, setCurrentClusterElementName] = useState<string | undefined>();
    const [clusterElementComponentOperations, setClusterElementComponentOperations] = useState<Array<WorkflowNodeType>>(
        []
    );

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {
        activeTab,
        currentComponent,
        currentNode,
        setActiveTab,
        setCurrentComponent,
        setCurrentNode,
        workflowNodeDetailsPanelOpen,
    } = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            activeTab: state.activeTab,
            currentComponent: state.currentComponent,
            currentNode: state.currentNode,
            setActiveTab: state.setActiveTab,
            setCurrentComponent: state.setCurrentComponent,
            setCurrentNode: state.setCurrentNode,
            workflowNodeDetailsPanelOpen: state.workflowNodeDetailsPanelOpen,
        }))
    );

    const {nodes, setDataPills, workflow, workflowNodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
            setDataPills: state.setDataPills,
            workflow: state.workflow,
            workflowNodes: state.workflowNodes,
        }))
    );

    const {clusterElementsCanvasOpen, rootClusterElementNodeData} = useWorkflowEditorStore(
        useShallow((state) => ({
            clusterElementsCanvasOpen: state.clusterElementsCanvasOpen,
            rootClusterElementNodeData: state.rootClusterElementNodeData,
        }))
    );

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
            environmentId: currentEnvironmentId,
            workflowId: workflow.id as string,
            workflowNodeName: currentNode?.clusterElementType
                ? (rootClusterElementNodeData?.workflowNodeName as string)
                : (currentNode?.workflowNodeName as string),
        },
        !!workflow.id && !!currentNode
    );

    const {data: currentClusterElementDefinition} = useGetClusterElementDefinitionQuery(
        {
            clusterElementName: currentNode?.clusterElementName as string,
            componentName: currentNode?.componentName as string,
            componentVersion: currentNode?.version as number,
        },
        !!currentNode?.clusterElementType
    );

    const deleteWorkflowNodeTestOutputMutation = useDeleteWorkflowNodeTestOutputMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: [...WorkflowNodeOutputKeys.workflowNodeOutputs, workflow.id],
            });
        },
    });

    const previousDataPillsRef = useRef<Array<DataPillType>>([]);

    const matchingOperation = useMemo(
        () =>
            [
                ...(currentComponentDefinition?.actions || []),
                ...(currentComponentDefinition?.triggers || []),
                ...(currentComponentDefinition?.clusterElements || []),
            ].find((operationDefinition) => operationDefinition?.name === currentOperationName),
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
        !!currentNode?.componentName && !!currentNode?.trigger && !!currentComponentDefinition
    );

    const fetchClusterElementDefinition = useCallback(
        async (operationName?: string) => {
            const clusterElementDefinitionRequest: GetComponentClusterElementDefinitionRequest = {
                clusterElementName: operationName ?? currentOperationName ?? currentNode?.operationName,
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

            return clusterElementDefinition;
        },
        [
            currentComponentDefinition?.name,
            currentComponentDefinition?.version,
            currentNode?.operationName,
            currentOperationName,
            queryClient,
        ]
    );

    const fetchActionDefinition = useCallback(
        async (operationName?: string) => {
            const actionDefinitionRequest: GetComponentActionDefinitionRequest = {
                actionName: operationName ?? currentOperationName ?? currentNode?.operationName,
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

            return actionDefinition;
        },
        [
            currentComponentDefinition?.name,
            currentComponentDefinition?.version,
            currentNode?.operationName,
            currentOperationName,
            queryClient,
        ]
    );

    const fetchTriggerDefinition = useCallback(
        async (operationName?: string) => {
            const triggerDefinitionRequest: GetComponentTriggerDefinitionRequest = {
                componentName: currentComponentDefinition?.name as string,
                componentVersion: currentComponentDefinition?.version as number,
                triggerName: operationName ?? currentOperationName ?? currentNode?.operationName,
            };

            const triggerDefinition = await queryClient.fetchQuery({
                queryFn: () => new TriggerDefinitionApi().getComponentTriggerDefinition(triggerDefinitionRequest),
                queryKey: TriggerDefinitionKeys.triggerDefinition(triggerDefinitionRequest),
            });

            if (triggerDefinition) {
                setCurrentActionDefinition(triggerDefinition);

                setCurrentActionFetched(true);
            }

            return triggerDefinition;
        },
        [
            currentComponentDefinition?.name,
            currentComponentDefinition?.version,
            currentNode?.operationName,
            currentOperationName,
            queryClient,
        ]
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
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            workflowNodeName: currentNodeName!,
        },
        !!currentNodeName &&
            currentNodeName !== 'manual' &&
            currentNodeName !== currentClusterElementName &&
            !currentNode?.clusterElementType
    );

    const clusterElementDisplayConditionsQuery = useGetClusterElementParameterDisplayConditionsQuery(
        {
            clusterElementType: currentNode?.clusterElementType || '',
            clusterElementWorkflowNodeName: currentNode?.workflowNodeName || '',
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            workflowNodeName: rootClusterElementNodeData?.workflowNodeName as string,
        },
        !!currentNode &&
            currentNodeName !== 'manual' &&
            currentNodeName === currentClusterElementName &&
            !!currentNode?.clusterElementType
    );

    const {data: workflowNodeParameterDisplayConditions} = displayConditionsQuery;
    const {data: clusterElementParameterDisplayConditions} = clusterElementDisplayConditionsQuery;

    const currentOperationDefinition = useMemo(() => {
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
        () => currentNode && nodes?.findIndex((node) => node.data.name === currentNode?.workflowNodeName),
        [currentNode, nodes]
    );

    const outputDefined =
        currentActionDefinition?.outputDefined ||
        currentTriggerDefinition?.outputDefined ||
        currentClusterElementDefinition?.outputDefined ||
        (currentOperationDefinition as TaskDispatcherDefinition)?.variablePropertiesDefined;
    const outputFunctionDefined =
        currentActionDefinition?.outputFunctionDefined ||
        currentTriggerDefinition?.outputFunctionDefined ||
        currentClusterElementDefinition?.outputSchemaDefined;

    const showOutputTab = useMemo(() => {
        if (currentNode?.clusterElementType) {
            return false;
        }

        if (currentOperationDefinition && 'variablePropertiesDefined' in currentOperationDefinition) {
            const taskDispatcher = currentOperationDefinition as TaskDispatcherDefinition;

            if (!taskDispatcher.variablePropertiesDefined) {
                return false;
            }
        }

        return true;
    }, [currentNode?.clusterElementType, currentOperationDefinition]);

    const currentWorkflowTrigger = useMemo(
        () => workflow.triggers?.find((trigger) => trigger.name === currentNode?.workflowNodeName),
        [workflow.triggers, currentNode]
    );

    const currentWorkflowTask = useMemo(
        () =>
            currentNode?.workflowNodeName
                ? getTaskDispatcherTask({
                      taskDispatcherId: currentNode.workflowNodeName,
                      tasks: workflow.tasks || [],
                  })
                : undefined,
        [workflow.tasks, currentNode]
    );

    const currentClusterElementsConnections = useMemo(() => {
        if (!rootClusterElementNodeData?.workflowNodeName) {
            return undefined;
        }

        const mainClusterRootTask = getTaskDispatcherTask({
            taskDispatcherId: rootClusterElementNodeData.workflowNodeName,
            tasks: workflow.tasks || [],
        });

        if (!mainClusterRootTask) {
            return undefined;
        }

        if (mainClusterRootTask.connections && currentNode?.clusterElementType) {
            return mainClusterRootTask.connections.filter(
                (connection) => connection.key === currentNode?.workflowNodeName
            );
        }

        if (currentNode?.clusterRoot && !currentNode?.isNestedClusterRoot && mainClusterRootTask.clusterElements) {
            const connections: ComponentConnection[] = [];
            const clusterElements = mainClusterRootTask.clusterElements;

            const extractConnection = (
                elementData: {name?: string},
                componentName?: string,
                componentVersion?: number
            ) => {
                if (!elementData || !elementData.name || !componentName || !componentVersion) {
                    return null;
                }

                return {
                    componentName,
                    componentVersion,
                    key: elementData.name,
                    required: false,
                    workflowNodeName: elementData.name,
                };
            };

            const extractConnectionsRecursively = (element: unknown): void => {
                if (!element || typeof element !== 'object') {
                    return;
                }

                if (Array.isArray(element)) {
                    element.forEach((item) => extractConnectionsRecursively(item));
                    return;
                }

                const elementObj = element as {
                    clusterElements?: unknown;
                    name?: string;
                    type?: string;
                };

                if (elementObj.type && elementObj.name) {
                    const typeParts = elementObj.type.split('/');
                    const componentName = typeParts[0];
                    const componentVersion = parseInt(typeParts[1]?.replace('v', '') || '1');

                    const connection = extractConnection(elementObj, componentName, componentVersion);
                    if (connection) {
                        connections.push(connection);
                    }
                }

                if (elementObj.clusterElements) {
                    const nestedElements = elementObj.clusterElements as Record<string, unknown>;
                    Object.values(nestedElements).forEach((nestedElement) => {
                        extractConnectionsRecursively(nestedElement);
                    });
                }
            };

            Object.values(clusterElements).forEach((element) => {
                extractConnectionsRecursively(element);
            });

            return connections.length > 0 ? connections : undefined;
        }

        return undefined;
    }, [
        currentNode?.clusterElementType,
        currentNode?.clusterRoot,
        currentNode?.isNestedClusterRoot,
        currentNode?.workflowNodeName,
        rootClusterElementNodeData?.workflowNodeName,
        workflow.tasks,
    ]);

    const currentWorkflowNodeConnections: ComponentConnection[] = useMemo(() => {
        if (currentNode?.clusterRoot && !currentNode?.isNestedClusterRoot) {
            return (
                currentWorkflowTask?.connections ||
                currentWorkflowTrigger?.connections ||
                currentClusterElementsConnections ||
                []
            );
        }

        const connections =
            currentWorkflowTask?.connections ||
            currentWorkflowTrigger?.connections ||
            currentClusterElementsConnections ||
            currentNode?.connections ||
            [];

        if (
            connections.length === 0 &&
            currentComponentDefinition?.connection &&
            currentComponentDefinition?.name &&
            currentComponentDefinition?.version &&
            currentNode?.workflowNodeName &&
            !currentNode?.clusterElementType
        ) {
            return [
                {
                    componentName: currentComponentDefinition.name,
                    componentVersion: currentComponentDefinition.version,
                    key: currentComponentDefinition.name,
                    required: currentComponentDefinition.connectionRequired || false,
                    workflowNodeName: currentNode.workflowNodeName,
                },
            ];
        }

        return connections;
    }, [
        currentClusterElementsConnections,
        currentComponentDefinition?.connection,
        currentComponentDefinition?.connectionRequired,
        currentComponentDefinition?.name,
        currentComponentDefinition?.version,
        currentNode?.clusterElementType,
        currentNode?.clusterRoot,
        currentNode?.connections,
        currentNode?.isNestedClusterRoot,
        currentNode?.workflowNodeName,
        currentWorkflowTask?.connections,
        currentWorkflowTrigger?.connections,
    ]);

    const nodeTabs = useMemo(
        () =>
            TABS.filter(({name}) => {
                if (name === 'connection') {
                    return (
                        (currentWorkflowNodeConnections.length > 0 ||
                            currentComponentDefinition?.connection !== undefined) &&
                        !currentNode?.clusterElementType
                    );
                }

                if (name === 'output') {
                    return showOutputTab;
                }

                if (name === 'properties') {
                    return currentNode?.taskDispatcher
                        ? currentTaskDispatcherDefinition?.properties?.length
                        : currentOperationProperties?.length;
                }

                return true;
            }),
        [
            currentComponentDefinition,
            currentNode,
            currentOperationProperties,
            currentTaskDispatcherDefinition,
            currentWorkflowNodeConnections,
            showOutputTab,
        ]
    );

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

    const filteredClusterElementOperations = useMemo(() => {
        if (currentComponentDefinition?.clusterElement && currentNode?.clusterElementType) {
            return currentComponentDefinition?.clusterElements?.filter(
                (clusterElement) =>
                    clusterElement.type === convertNameToSnakeCase(currentNode.clusterElementType as string)
            );
        }

        return currentComponentDefinition?.clusterElements;
    }, [
        currentComponentDefinition?.clusterElement,
        currentComponentDefinition?.clusterElements,
        currentNode?.clusterElementType,
    ]);

    const calculatedDataPills = useMemo(() => {
        if (!previousComponentDefinitions || !workflowNodeOutputs) {
            return [];
        }

        let filteredNodeNames = workflowNodeOutputs?.map((output) => output.workflowNodeName) || [];

        if (currentNode?.conditionData) {
            const parentConditionTask = currentNode.conditionData.conditionId
                ? getTaskDispatcherTask({
                      taskDispatcherId: currentNode.conditionData.conditionId,
                      tasks: workflow.tasks || [],
                  })
                : undefined;

            if (!parentConditionTask) {
                return [];
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
            const parentBranchTask = currentNode.branchData.branchId
                ? getTaskDispatcherTask({
                      taskDispatcherId: currentNode.branchData.branchId,
                      tasks: workflow.tasks || [],
                  })
                : undefined;

            if (!parentBranchTask || !parentBranchTask.parameters) {
                return [];
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

        const componentProperties: Array<ComponentPropertiesType> = previousComponentDefinitions?.map(
            (componentDefinition, index) => {
                const outputSchemaDefinition: PropertyAllType | undefined =
                    workflowNodeOutputs[index]?.outputResponse?.outputSchema;

                const properties = outputSchemaDefinition?.properties?.length
                    ? outputSchemaDefinition.properties
                    : outputSchemaDefinition?.items;

                return {
                    componentDefinition,
                    properties,
                    workflowNodeName: workflowNodeOutputs[index]?.workflowNodeName,
                };
            }
        );

        const dataPills = getDataPillsFromProperties(componentProperties!, filteredNodeNames);

        return dataPills.flat(Infinity);
    }, [
        previousComponentDefinitions,
        workflowNodeOutputs,
        currentNode?.conditionData,
        currentNode?.branchData,
        workflow.tasks,
    ]);

    const handleOperationSelectChange = useCallback(
        async (newOperationName: string) => {
            if (currentOperationName === newOperationName) {
                return;
            }

            setCurrentOperationName(newOperationName);

            let newOperationDefinition: ActionDefinition | TriggerDefinition | ClusterElementDefinition | undefined;

            if (!currentComponentDefinition || !currentComponent) {
                return;
            }

            if (currentNode?.trigger && !isClusterElement) {
                newOperationDefinition = await fetchTriggerDefinition(newOperationName);
            } else if (clusterElementsCanvasOpen && !!isClusterElement) {
                newOperationDefinition = await fetchClusterElementDefinition(newOperationName);
            } else {
                newOperationDefinition = await fetchActionDefinition(newOperationName);
            }

            if (!newOperationDefinition) {
                console.error(`newOperationDefinition not found for: ${newOperationName}`);

                return;
            }

            await deleteWorkflowNodeTestOutputMutation.mutateAsync({
                environmentId: currentEnvironmentId,
                id: workflow.id!,
                workflowNodeName: currentNode!.name,
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowNodeDynamicPropertyKeys.workflowNodeDynamicProperties,
            });

            queryClient.invalidateQueries({
                queryKey: ClusterElementDynamicPropertyKeys.clusterElementDynamicProperties,
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowNodeOptionKeys.workflowNodeOptions,
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowNodeOptionKeys.clusterElementNodeOptions,
            });

            const {componentName, description, label, workflowNodeName} = currentComponent;

            const nodeData: NodeDataType = {
                componentName,
                description,
                label,
                name: workflowNodeName || currentNode?.workflowNodeName || '',
                operationName: newOperationName,
                parameters: getParametersWithDefaultValues({
                    properties: newOperationDefinition.properties as Array<PropertyAllType>,
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
                    currentOperationProperties: newOperationDefinition.properties as Array<PropertyAllType>,
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

            if (currentNode?.clusterElementType && currentNode?.clusterElementType !== undefined) {
                saveClusterElementFieldChange({
                    currentComponentDefinition,
                    currentOperationProperties: newOperationDefinition.properties as Array<PropertyAllType>,
                    fieldUpdate: {
                        field: 'operation',
                        value: newOperationName,
                    },
                    invalidateWorkflowQueries,
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
                            properties: newOperationDefinition.properties as Array<PropertyAllType>,
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
                            properties: newOperationDefinition.properties as Array<PropertyAllType>,
                        }),
                        type: `${componentName}/v${currentComponentDefinition.version}/${newOperationName}`,
                        workflowNodeName,
                    });
                },
                updateWorkflowMutation,
            });
        },
        [
            currentEnvironmentId,
            currentOperationName,
            currentComponentDefinition,
            currentComponent,
            currentNode,
            clusterElementsCanvasOpen,
            isClusterElement,
            deleteWorkflowNodeTestOutputMutation,
            workflow.id,
            queryClient,
            invalidateWorkflowQueries,
            updateWorkflowMutation,
            fetchTriggerDefinition,
            fetchClusterElementDefinition,
            fetchActionDefinition,
            currentNodeIndex,
            setCurrentComponent,
            setCurrentNode,
        ]
    );

    const handlePanelClose = useCallback(() => {
        useWorkflowNodeDetailsPanelStore.getState().reset();
    }, []);

    const getNodeVersion = useCallback((node: typeof currentWorkflowNode): string => {
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
    }, []);

    // Set current node name
    useEffect(() => {
        if (currentOperationDefinition?.properties) {
            setCurrentOperationProperties(currentOperationDefinition?.properties);
        }

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
    }, [currentNode?.name, currentOperationDefinition?.properties, isClusterElement]);

    // Set currentOperationProperties depending if the current node is a trigger or an action
    useEffect(() => {
        if (currentOperationDefinition?.properties) {
            setCurrentOperationProperties(currentOperationDefinition?.properties);
        }
    }, [currentOperationDefinition?.properties]);

    // Set data pills only when the calculated data pills change
    useEffect(() => {
        const currentDataPillsString = JSON.stringify(calculatedDataPills);
        const previousDataPillsString = JSON.stringify(previousDataPillsRef.current);

        if (currentDataPillsString !== previousDataPillsString) {
            previousDataPillsRef.current = calculatedDataPills;
            setDataPills(calculatedDataPills);
        }
    }, [calculatedDataPills, setDataPills]);

    // Tab switching logic
    useEffect(() => {
        if (
            activeTab === 'connection' &&
            ((currentWorkflowNodeConnections.length === 0 && currentComponentDefinition?.connection === undefined) ||
                currentNode?.clusterElementType)
        ) {
            setActiveTab('description');

            return;
        }

        if (currentComponentDefinition?.name === 'manual') {
            setActiveTab('description');

            return;
        }

        if (
            activeTab === 'properties' &&
            ((!currentNode?.trigger && currentActionFetched) || (currentNode?.trigger && currentTriggerFetched)) &&
            !currentOperationProperties
        ) {
            setActiveTab('description');

            return;
        }

        if (activeTab === 'output' && !showOutputTab) {
            setActiveTab('description');

            return;
        }

        if (activeTab === 'properties' && !operationDataMissing && !currentOperationProperties?.length) {
            setActiveTab('description');

            return;
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        activeTab,
        currentActionDefinition?.outputDefined,
        currentActionFetched,
        currentOperationProperties?.length,
        currentComponentDefinition?.name,
        currentWorkflowNodeConnections.length,
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

        const currentClusterElementConnectionId = workflowTestConfigurationConnections?.find(
            (connection) => connection.workflowConnectionKey === currentNode?.workflowNodeName
        )?.connectionId;

        const currentMainRootElementConnectionId = workflowTestConfigurationConnections?.find(
            (connection) =>
                connection.workflowNodeName === rootClusterElementNodeData?.workflowNodeName &&
                !connection.workflowConnectionKey.includes('_')
        )?.connectionId;

        if (currentNode.operationName && currentOperationName) {
            updatedNode = {
                ...updatedNode,
                operationName: currentOperationName,
                triggerType: currentTriggerDefinition?.type,
                type: `${currentComponent?.componentName}/v${currentComponentDefinition?.version}/${currentOperationName}`,
            };
        }

        if (currentWorkflowNodeConnections.length) {
            if (currentNode?.clusterElementType && currentNode?.clusterElementType !== undefined) {
                updatedNode = {
                    ...updatedNode,
                    connectionId: currentClusterElementConnectionId,
                    connections: currentWorkflowNodeConnections,
                };
            } else if (currentNode?.workflowNodeName === rootClusterElementNodeData?.workflowNodeName) {
                updatedNode = {
                    ...updatedNode,
                    connectionId: currentMainRootElementConnectionId,
                    connections: currentWorkflowNodeConnections,
                };
            } else {
                updatedNode = {
                    ...updatedNode,
                    connectionId: workflowTestConfigurationConnections
                        ? workflowTestConfigurationConnections[0]?.connectionId
                        : undefined,
                    connections: currentWorkflowNodeConnections,
                };
            }
        }

        if (!isEqual(updatedNode, currentNode)) {
            setCurrentNode(updatedNode);

            setCurrentComponent(updatedNode);
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
        if (!clusterElementsCanvasOpen || !workflow.definition) {
            return;
        }

        const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

        const mainClusterRootTask = rootClusterElementNodeData?.workflowNodeName
            ? getTaskDispatcherTask({
                  taskDispatcherId: rootClusterElementNodeData.workflowNodeName,
                  tasks: workflowDefinitionTasks,
              })
            : undefined;

        if (!mainClusterRootTask) {
            return;
        }

        const currentRootClusterTaskClusterElements = mainClusterRootTask?.clusterElements;

        if (currentRootClusterTaskClusterElements) {
            const clusterElementsWorkflowNodeTypes = extractClusterElementComponentOperations(
                currentRootClusterTaskClusterElements
            );

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
            if (currentNode?.clusterRoot && !currentNode.isNestedClusterRoot) {
                currentWorkflowNode = workflowNodes.find(
                    (workflowNodeType) => workflowNodeType.workflowNodeName === currentNode?.workflowNodeName
                );
            } else if (clusterElementComponentOperations) {
                currentWorkflowNode = clusterElementComponentOperations.find(
                    (workflowNodeType) => workflowNodeType.workflowNodeName === currentNode?.workflowNodeName
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
        if (
            currentNode &&
            (workflowNodeParameterDisplayConditions?.displayConditions ||
                clusterElementParameterDisplayConditions?.displayConditions)
        ) {
            setCurrentNode({
                ...currentNode,
                displayConditions:
                    workflowNodeParameterDisplayConditions?.displayConditions ||
                    clusterElementParameterDisplayConditions?.displayConditions,
            });
        }

        if (
            currentComponent &&
            (workflowNodeParameterDisplayConditions?.displayConditions ||
                clusterElementParameterDisplayConditions?.displayConditions)
        ) {
            if (currentComponent.workflowNodeName === currentNode?.name) {
                setCurrentComponent({
                    ...currentComponent,
                    displayConditions:
                        workflowNodeParameterDisplayConditions?.displayConditions ||
                        clusterElementParameterDisplayConditions?.displayConditions,
                });
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        clusterElementParameterDisplayConditions?.displayConditions,
        currentNode?.name,
        workflowNodeParameterDisplayConditions?.displayConditions,
    ]);

    // Fetch current action definition when operation changes
    useEffect(() => {
        if (currentActionDefinition?.name === currentOperationName) {
            return;
        }

        if (!currentOperationName || !currentComponentDefinition) {
            setCurrentActionDefinition(undefined);

            return;
        }

        if (
            clusterElementsCanvasOpen &&
            currentComponentDefinition?.clusterElement &&
            currentNode?.parentClusterRootId
        ) {
            if (matchingOperation) {
                fetchClusterElementDefinition();
            } else {
                setCurrentActionDefinition(undefined);
            }
        } else {
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
            key={`${currentNode?.workflowNodeName}-${currentOperationName}`}
        >
            {currentNode?.workflowNodeName && currentWorkflowNode && (
                <div className="flex h-full flex-col divide-y divide-muted bg-background">
                    <header className="flex items-start justify-between p-4 text-lg font-medium">
                        <div className="flex items-center gap-2">
                            {currentWorkflowNode.icon && (
                                <InlineSVG
                                    className="size-8"
                                    loader={<LoadingIcon className="ml-0 mr-2 size-6 text-muted-foreground" />}
                                    src={currentWorkflowNode.icon}
                                />
                            )}

                            <div className="flex flex-col items-start">
                                <div className="flex items-center gap-2">
                                    <span className="text-lg font-semibold">{currentNode?.label}</span>

                                    {currentWorkflowNode.description && (
                                        <Tooltip delayDuration={500}>
                                            <TooltipTrigger>
                                                <InfoIcon className="size-4 shrink-0" />
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
                                </div>

                                <span className="text-sm text-muted-foreground">({currentNode?.workflowNodeName})</span>
                            </div>
                        </div>

                        {closeButton ? (
                            closeButton
                        ) : (
                            <Button
                                aria-label="Close the node details dialog"
                                icon={<XIcon aria-hidden="true" />}
                                onClick={handlePanelClose}
                                size="icon"
                                variant="ghost"
                            />
                        )}
                    </header>

                    <main className="flex h-full flex-col overflow-hidden">
                        {!!currentWorkflowNodeOperations?.length && operationDataMissing && (
                            <FieldsetSkeleton bottomBorder label="Actions" />
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
                                        : !!currentNode?.clusterElementType &&
                                            currentNode?.workflowNodeName !==
                                                rootClusterElementNodeData?.workflowNodeName
                                          ? currentComponentDefinition?.description
                                          : currentActionDefinition?.description
                                }
                                handleValueChange={handleOperationSelectChange}
                                operations={
                                    (currentNode?.trigger
                                        ? currentComponentDefinition?.triggers
                                        : !!currentNode?.clusterElementType &&
                                            currentNode?.workflowNodeName !==
                                                rootClusterElementNodeData?.workflowNodeName
                                          ? filteredClusterElementOperations
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
                                            'grow justify-center whitespace-nowrap rounded-none border-0 border-b border-border bg-content-onsurface-primary py-5 text-sm font-medium text-content-neutral-secondary hover:border-stroke-brand-primary hover:text-content-brand-primary focus:border-stroke-brand-primary focus:text-content-brand-primary focus:outline-none',
                                            activeTab === tab?.name &&
                                                'border-stroke-brand-primary text-content-brand-primary hover:text-content-brand-primary'
                                        )}
                                        key={tab.name}
                                        label={tab.label}
                                        name={tab.name}
                                        onClick={() => setActiveTab(tab.name)}
                                        variant="ghost"
                                    />
                                ))}
                            </div>
                        )}

                        {currentNode.componentName !== 'manual' && !tabDataExists && (
                            <div className="flex justify-center space-x-2 border-b border-border p-2">
                                {Array.from({length: 4}).map((_, index) => (
                                    <Skeleton className="h-6 w-1/4" key={index} />
                                ))}
                            </div>
                        )}

                        <ScrollArea className="h-full max-w-workflow-node-details-panel-width bg-surface-main">
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
                                        <DescriptionTabSkeleton />
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
                                        <PropertiesTabSkeleton />
                                    ))}

                                {activeTab === 'output' && (
                                    <OutputTab
                                        connectionMissing={
                                            (currentComponentDefinition?.connectionRequired ?? false) &&
                                            !workflowTestConfigurationConnections?.length
                                        }
                                        currentNode={currentNode}
                                        key={`${currentNode?.componentName}-${currentNode?.type}_output`}
                                        outputDefined={outputDefined}
                                        outputFunctionDefined={outputFunctionDefined}
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
