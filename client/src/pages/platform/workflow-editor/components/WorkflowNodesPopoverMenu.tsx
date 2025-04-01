import {Input} from '@/components/ui/input';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ScrollArea} from '@/components/ui/scroll-area';
import {
    ClusterElementDefinitionBasic,
    ComponentDefinition,
    ComponentDefinitionApi,
} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {ClickedDefinitionType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import {PropsWithChildren, useCallback, useEffect, useMemo, useState} from 'react';
import {useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {AgentDataType} from '../nodes/AIAgentNode';
import {useWorkflowMutation} from '../providers/workflowMutationProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getTaskDispatcherContext from '../utils/getTaskDispatcherContext';
import handleTaskDispatcherClick from '../utils/handleTaskDispatcherClick';
import WorkflowNodesPopoverMenuComponentList from './WorkflowNodesPopoverMenuComponentList';
import WorkflowNodesPopoverMenuOperationList from './WorkflowNodesPopoverMenuOperationList';

interface WorkflowNodesPopoverMenuProps extends PropsWithChildren {
    agentData?: AgentDataType;
    conditionId?: string;
    edgeId?: string;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    loopId?: string;
    nodeIndex?: number;
    setAgentData?: (data: AgentDataType) => void;
    sourceData?: ClusterElementDefinitionBasic[];
    sourceNodeId: string;
}

const WorkflowNodesPopoverMenu = ({
    agentData,
    children,
    conditionId,
    edgeId,
    hideActionComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    loopId,
    nodeIndex,
    setAgentData,
    sourceData,
    sourceNodeId,
}: WorkflowNodesPopoverMenuProps) => {
    const [actionPanelOpen, setActionPanelOpen] = useState(false);
    const [componentDefinitionToBeAdded, setComponentDefinitionToBeAdded] = useState<ComponentDefinition | null>(null);
    const [popoverOpen, setPopoverOpen] = useState(false);
    const [trigger, setTrigger] = useState(false);

    const {componentDefinitions, taskDispatcherDefinitions, workflow} = useWorkflowDataStore();

    const {edges, nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
        }))
    );

    const {updateWorkflowMutation} = useWorkflowMutation();

    const queryClient = useQueryClient();

    const {projectId} = useParams();

    const memoizedComponentDefinitions = useMemo(() => componentDefinitions, [componentDefinitions]);
    const memoizedTaskDispatcherDefinitions = useMemo(() => taskDispatcherDefinitions, [taskDispatcherDefinitions]);
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

    const handleClusterElementClick = (data: ClusterElementDefinitionBasic) => {
        if (agentData && setAgentData) {
            let value = data.componentName;

            if (data.type === 'TOOLS') {
                value = `${data.componentName}#${data.name}`;
            }

            setAgentData({...agentData, [data.type]: value});
        }
    };

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
                                <Input
                                    disabled
                                    // value={filter}
                                    name="workflowNodeFilter"
                                    // onChange={(event) => setFilter(event.target.value)}
                                    placeholder="Search AI models"
                                />
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
                            handleComponentClick={handleComponentClick}
                            hideActionComponents={hideActionComponents}
                            hideTaskDispatchers={hideTaskDispatchers}
                            hideTriggerComponents={hideTriggerComponents}
                            selectedComponentName={componentDefinitionToBeAdded?.name}
                        />
                    )}

                    {!sourceData && actionPanelOpen && componentDefinitionToBeAdded && (
                        <WorkflowNodesPopoverMenuOperationList
                            componentDefinition={componentDefinitionToBeAdded}
                            conditionId={conditionId}
                            edgeId={edgeId}
                            loopId={loopId}
                            setPopoverOpen={setPopoverOpen}
                            sourceNodeId={sourceNodeId}
                            // taskDispatcherContext={taskDispatcherContext}
                            trigger={trigger}
                        />
                    )}

                    {!sourceData && (!memoizedComponentDefinitions || !memoizedTaskDispatcherDefinitions) && (
                        <div className="px-3 py-2 text-xs">Something went wrong.</div>
                    )}
                </div>
            </PopoverContent>
        </Popover>
    );
};

export default WorkflowNodesPopoverMenu;
