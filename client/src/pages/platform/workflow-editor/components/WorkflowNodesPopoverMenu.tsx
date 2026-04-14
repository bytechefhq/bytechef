import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ComponentDefinition, ComponentDefinitionApi} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {ClickedDefinitionType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {MouseEvent, PropsWithChildren, useCallback, useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getTaskDispatcherContext from '../utils/getTaskDispatcherContext';
import handleTaskDispatcherClick from '../utils/handleTaskDispatcherClick';
import {TASK_DISPATCHER_CONFIG} from '../utils/taskDispatcherConfig';
import WorkflowNodesPopoverMenuComponentList from './WorkflowNodesPopoverMenuComponentList';
import WorkflowNodesPopoverMenuOperationList from './WorkflowNodesPopoverMenuOperationList';

interface WorkflowNodesPopoverMenuProps extends PropsWithChildren {
    clusterElementType?: string;
    edgeId?: string;
    hideActionComponents?: boolean;
    hideClusterElementComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    multipleClusterElementsNode?: boolean;
    nodeIndex?: number;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    sourceNodeId: string;
    sourceNodeName?: string;
}

const WorkflowNodesPopoverMenu = ({
    children,
    clusterElementType,
    edgeId,
    hideActionComponents = false,
    hideClusterElementComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    multipleClusterElementsNode = false,
    nodeIndex,
    onOpenChange: externalOnOpenChange,
    open: externalOpen,
    sourceNodeId,
    sourceNodeName,
}: WorkflowNodesPopoverMenuProps) => {
    const [componentDefinitionToBeAdded, setComponentDefinitionToBeAdded] = useState<ComponentDefinition | null>(null);
    const [internalOpen, setInternalOpen] = useState(false);
    const [trigger, setTrigger] = useState(false);

    const actionPanelOpen = !!componentDefinitionToBeAdded?.name;

    const popoverOpen = externalOpen ?? internalOpen;
    const setPopoverOpen = externalOnOpenChange ?? setInternalOpen;

    const workflow = useWorkflowDataStore((state) => state.workflow);
    const {edges, nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
        }))
    );

    const {updateWorkflowMutation} = useWorkflowEditor();

    const queryClient = useQueryClient();

    const sourceNode = useMemo(() => nodes.find((node) => node.id === sourceNodeId), [sourceNodeId, nodes]);

    const handleActionPanelClose = useCallback(() => {
        setComponentDefinitionToBeAdded(null);
    }, []);

    const handlePopoverOpenChange = useCallback(
        (open: boolean) => {
            setPopoverOpen(open);

            if (!open) {
                handleActionPanelClose();
            }
        },
        [handleActionPanelClose, setPopoverOpen]
    );

    const handleStopPropagation = useCallback((event: MouseEvent) => event.stopPropagation(), []);

    const handlePasteClose = useCallback(() => setPopoverOpen(false), [setPopoverOpen]);

    const handleComponentClick = useCallback(
        async (clickedItem: ClickedDefinitionType) => {
            const {componentVersion, name, taskDispatcher, trigger, version} = clickedItem;

            if (taskDispatcher) {
                if (!updateWorkflowMutation) {
                    return;
                }

                const edge = edges.find((edge) => edge.id === edgeId);

                const taskDispatcherContext = getTaskDispatcherContext({
                    edge: edge,
                    node: edge?.type === 'workflow' ? undefined : sourceNode,
                    nodes: nodes,
                });

                await handleTaskDispatcherClick({
                    edge,
                    queryClient,
                    sourceNodeId,
                    taskDispatcherContext,
                    taskDispatcherDefinition: clickedItem,
                    taskDispatcherName: name as keyof typeof TASK_DISPATCHER_CONFIG,
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

    useEffect(() => {
        return () => {
            setComponentDefinitionToBeAdded(null);
            setPopoverOpen(false);
            setTrigger(false);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <Popover
            key={`${sourceNodeId}-popoverMenu-${nodeIndex}`}
            modal
            onOpenChange={handlePopoverOpenChange}
            open={popoverOpen}
        >
            <PopoverTrigger asChild onClick={handleStopPropagation}>
                {children}
            </PopoverTrigger>

            <PopoverContent
                align="start"
                className={twMerge(
                    'flex rounded-lg p-0 will-change-auto',
                    actionPanelOpen ? 'w-workflow-nodes-popover-menu-width' : 'w-node-popover-width'
                )}
                onClick={handleStopPropagation}
                side="right"
                sideOffset={-34}
            >
                <div className="nowheel flex w-full rounded-lg bg-surface-neutral-secondary">
                    <WorkflowNodesPopoverMenuComponentList
                        actionPanelOpen={actionPanelOpen}
                        clusterElementType={clusterElementType}
                        edgeId={edgeId}
                        handleComponentClick={handleComponentClick}
                        hideActionComponents={hideActionComponents}
                        hideClusterElementComponents={hideClusterElementComponents}
                        hideTaskDispatchers={hideTaskDispatchers}
                        hideTriggerComponents={hideTriggerComponents}
                        onPasteClose={handlePasteClose}
                        selectedComponentName={componentDefinitionToBeAdded?.name}
                        showPaste
                        sourceNodeId={sourceNodeId}
                        updateWorkflowMutation={updateWorkflowMutation}
                    />

                    {actionPanelOpen && componentDefinitionToBeAdded && (
                        <WorkflowNodesPopoverMenuOperationList
                            clusterElementType={clusterElementType}
                            componentDefinition={componentDefinitionToBeAdded}
                            edgeId={edgeId}
                            multipleClusterElementsNode={multipleClusterElementsNode}
                            setPopoverOpen={setPopoverOpen}
                            sourceNodeId={sourceNodeId}
                            sourceNodeName={sourceNodeName}
                            trigger={trigger}
                        />
                    )}
                </div>
            </PopoverContent>
        </Popover>
    );
};

export default WorkflowNodesPopoverMenu;
