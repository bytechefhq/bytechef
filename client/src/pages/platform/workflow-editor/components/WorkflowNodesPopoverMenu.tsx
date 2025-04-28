import {Input} from '@/components/ui/input';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ScrollArea} from '@/components/ui/scroll-area';
import {
    ClusterElementDefinitionBasic,
    ComponentDefinition,
    ComponentDefinitionApi,
} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {ClickedDefinitionType, ClusterElementsType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import {PropsWithChildren, useCallback, useEffect, useMemo, useState} from 'react';
import {useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {useWorkflowMutation} from '../providers/workflowMutationProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getFormattedClusterElementName from '../utils/getFormattedClusterElementName';
import getTaskDispatcherContext from '../utils/getTaskDispatcherContext';
import handleTaskDispatcherClick from '../utils/handleTaskDispatcherClick';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import WorkflowNodesPopoverMenuComponentList from './WorkflowNodesPopoverMenuComponentList';
import WorkflowNodesPopoverMenuOperationList from './WorkflowNodesPopoverMenuOperationList';

type ClusterElementsDefinitionType = 'CHAT_MEMORY' | 'MODEL' | 'RAG';
type StoredClusterElementsType = 'chatMemory' | 'model' | 'rag';

interface WorkflowNodesPopoverMenuProps extends PropsWithChildren {
    clusterElementsData?: ClusterElementsType;
    edgeId?: string;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    nodeIndex?: number;
    setClusterElementsData?: (data: ClusterElementsType) => void;
    sourceData?: ClusterElementDefinitionBasic[];
    sourceNodeId: string;
}

const WorkflowNodesPopoverMenu = ({
    children,
    clusterElementsData,
    edgeId,
    hideActionComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    nodeIndex,
    setClusterElementsData,
    sourceData,
    sourceNodeId,
}: WorkflowNodesPopoverMenuProps) => {
    const [actionPanelOpen, setActionPanelOpen] = useState(false);
    const [componentDefinitionToBeAdded, setComponentDefinitionToBeAdded] = useState<ComponentDefinition | null>(null);
    const [popoverOpen, setPopoverOpen] = useState(false);
    const [trigger, setTrigger] = useState(false);

    const {workflow} = useWorkflowDataStore();

    const {edges, nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
        }))
    );

    const {updateWorkflowMutation} = useWorkflowMutation();

    const queryClient = useQueryClient();

    const {projectId} = useParams();

    const sourceNode = useMemo(() => nodes.find((node) => node.id === sourceNodeId), [sourceNodeId, nodes]);

    const handleActionPanelClose = useCallback(() => {
        setActionPanelOpen(false);

        setComponentDefinitionToBeAdded(null);
    }, []);

    const handleComponentClick = useCallback(
        async (clickedItem: ClickedDefinitionType) => {
            const {componentVersion, name, taskDispatcher, trigger, version} = clickedItem;

            if (taskDispatcher) {
                const edge = edges.find((edge) => edge.id === edgeId);

                const taskDispatcherContext = getTaskDispatcherContext({
                    edge: edge,
                    node: edge?.type === 'workflow' ? undefined : sourceNode,
                    nodes: nodes,
                });

                await handleTaskDispatcherClick({
                    edge: !!edge,
                    projectId: +projectId!,
                    queryClient,
                    sourceNodeId,
                    taskDispatcherContext,
                    taskDispatcherDefinition: clickedItem,
                    taskDispatcherName: name as 'condition' | 'loop',
                    updateWorkflowMutation,
                    workflow,
                });

                setPopoverOpen(false);

                return;
            }

            if (trigger) {
                setTrigger(true);
            }

            const clickedComponentDefinition = await queryClient.fetchQuery({
                queryFn: () =>
                    new ComponentDefinitionApi().getComponentDefinition({
                        componentName: name,
                        componentVersion: componentVersion || version,
                    }),
                queryKey: ComponentDefinitionKeys.componentDefinition({
                    componentName: name,
                    componentVersion: componentVersion || version,
                }),
            });

            if (clickedComponentDefinition) {
                setComponentDefinitionToBeAdded(clickedComponentDefinition);
            }
        },
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [sourceNodeId, nodeIndex]
    );

    const handleClusterElementClick = useCallback(
        (data: ClusterElementDefinitionBasic) => {
            if (!clusterElementsData || !sourceNode) return;

            const updatedClusterElementsData: ClusterElementsType = {
                chatMemory: clusterElementsData.chatMemory,
                model: clusterElementsData.model,
                rag: clusterElementsData.rag,
                tools: [...(clusterElementsData.tools || [])],
            };

            const propertyMap: Record<ClusterElementsDefinitionType, StoredClusterElementsType> = {
                CHAT_MEMORY: 'chatMemory',
                MODEL: 'model',
                RAG: 'rag',
            };

            if (data.type === 'TOOLS') {
                updatedClusterElementsData.tools = [
                    ...(clusterElementsData.tools || []),
                    {
                        label: data.title,
                        name: getFormattedClusterElementName(data.name, 'tools'),
                        parameters: {},
                        type: `${data.componentName}/v${data.componentVersion}/${data.name}`,
                    },
                ];
            } else {
                if (data.type in propertyMap) {
                    updatedClusterElementsData[propertyMap[data.type as ClusterElementsDefinitionType]] = {
                        label: data.title,
                        name: getFormattedClusterElementName(
                            data.componentName,
                            propertyMap[data.type as ClusterElementsDefinitionType]
                        ),
                        parameters: {},
                        type: `${data.componentName}/v${data.componentVersion}/${propertyMap[data.type as ClusterElementsDefinitionType]}`,
                    };
                }
            }

            setClusterElementsData?.(updatedClusterElementsData);

            saveWorkflowDefinition({
                nodeData: {
                    ...sourceNode.data,
                    clusterElements: updatedClusterElementsData,
                    componentName: String(sourceNode.data.componentName),
                    name: String(sourceNode.data.name),
                    workflowNodeName: String(sourceNode.data.workflowNodeName),
                },
                projectId: +projectId!,
                queryClient,
                updateWorkflowMutation,
            });
        },
        [clusterElementsData, projectId, queryClient, setClusterElementsData, sourceNode, updateWorkflowMutation]
    );

    useEffect(() => {
        if (componentDefinitionToBeAdded?.name) {
            setActionPanelOpen(true);
        }
    }, [componentDefinitionToBeAdded?.name]);

    useEffect(() => {
        return () => {
            setActionPanelOpen(false);
            setComponentDefinitionToBeAdded(null);
            setPopoverOpen(false);
            setTrigger(false);
        };
    }, []);

    return (
        <Popover
            key={`${sourceNodeId}-popoverMenu-${nodeIndex}`}
            modal
            onOpenChange={(open) => {
                setPopoverOpen(open);

                if (!open) {
                    handleActionPanelClose();
                }
            }}
            open={popoverOpen}
        >
            <PopoverTrigger asChild onClick={(event) => event.stopPropagation()}>
                {children}
            </PopoverTrigger>

            <PopoverContent
                align="start"
                className={twMerge(
                    'flex rounded-lg p-0 will-change-auto',
                    actionPanelOpen ? 'w-workflow-nodes-popover-menu-width' : 'w-node-popover-width'
                )}
                onClick={(event) => event.stopPropagation()}
                side="right"
                sideOffset={-34}
            >
                <div className="nowheel flex w-full rounded-lg bg-surface-neutral-secondary">
                    {sourceData && sourceData.length > 0 && (
                        <div className="flex w-full flex-col">
                            <header className="flex items-center gap-1 rounded-t-lg bg-white p-3 text-center">
                                <Input disabled name="workflowNodeFilter" placeholder="Search AI models" />
                            </header>

                            <ScrollArea className="w-full overflow-y-auto">
                                <ul className="h-96 space-y-2 rounded-br-lg bg-muted p-3">
                                    {sourceData?.map((data, index) => (
                                        <li
                                            className="flex cursor-pointer items-center gap-2 space-y-1 rounded border-2 border-transparent bg-white px-2 py-1 hover:border-blue-200"
                                            key={index}
                                            onClick={() => handleClusterElementClick(data)}
                                        >
                                            {data.icon ? data.icon : <ComponentIcon />}

                                            <div className="flex flex-col gap-2 text-start">
                                                <div>
                                                    <span className="text-sm font-bold">{data.componentName}</span>

                                                    {data.type === 'TOOLS' && (
                                                        <span className="text-xs"> #{data.name}</span>
                                                    )}
                                                </div>

                                                <p className="break-words text-xs text-muted-foreground">
                                                    {data.description ? data.description : 'Description'}
                                                </p>
                                            </div>
                                        </li>
                                    ))}
                                </ul>
                            </ScrollArea>
                        </div>
                    )}

                    {!sourceData && (
                        <WorkflowNodesPopoverMenuComponentList
                            actionPanelOpen={actionPanelOpen}
                            edgeId={edgeId}
                            handleComponentClick={handleComponentClick}
                            hideActionComponents={hideActionComponents}
                            hideTaskDispatchers={hideTaskDispatchers}
                            hideTriggerComponents={hideTriggerComponents}
                            selectedComponentName={componentDefinitionToBeAdded?.name}
                            sourceNodeId={sourceNodeId}
                        />
                    )}

                    {!sourceData && actionPanelOpen && componentDefinitionToBeAdded && (
                        <WorkflowNodesPopoverMenuOperationList
                            componentDefinition={componentDefinitionToBeAdded}
                            edgeId={edgeId}
                            setPopoverOpen={setPopoverOpen}
                            sourceNodeId={sourceNodeId}
                            trigger={trigger}
                        />
                    )}
                </div>
            </PopoverContent>
        </Popover>
    );
};

export default WorkflowNodesPopoverMenu;
