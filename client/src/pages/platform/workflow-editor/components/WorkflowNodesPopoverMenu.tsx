import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {
    ClusterElementDefinitionApi,
    ClusterElementDefinitionBasic,
    ComponentDefinition,
    ComponentDefinitionApi,
} from '@/shared/middleware/platform/configuration';
import {ClusterElementDefinitionKeys} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {
    ComponentDefinitionKeys,
    useGetComponentDefinitionQuery,
} from '@/shared/queries/platform/componentDefinitions.queries';
import {ClickedDefinitionType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {PropsWithChildren, useCallback, useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {convertNameToCamelCase} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {useWorkflowMutation} from '../providers/workflowMutationProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import getTaskDispatcherContext from '../utils/getTaskDispatcherContext';
import handleClusterElementClick from '../utils/handleClusterElementClick';
import handleTaskDispatcherClick from '../utils/handleTaskDispatcherClick';
import WorkflowNodesPopoverMenuComponentList from './WorkflowNodesPopoverMenuComponentList';
import WorkflowNodesPopoverMenuOperationList from './WorkflowNodesPopoverMenuOperationList';

interface WorkflowNodesPopoverMenuProps extends PropsWithChildren {
    clusterElementType?: string;
    edgeId?: string;
    hideActionComponents?: boolean;
    hideClusterElementComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    nodeIndex?: number;
    sourceData?: ClusterElementDefinitionBasic[];
    sourceNodeId: string;
}

const WorkflowNodesPopoverMenu = ({
    children,
    clusterElementType,
    edgeId,
    hideActionComponents = false,
    hideClusterElementComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    nodeIndex,
    sourceData,
    sourceNodeId,
}: WorkflowNodesPopoverMenuProps) => {
    const [actionPanelOpen, setActionPanelOpen] = useState(false);
    const [componentDefinitionToBeAdded, setComponentDefinitionToBeAdded] = useState<ComponentDefinition | null>(null);
    const [popoverOpen, setPopoverOpen] = useState(false);
    const [trigger, setTrigger] = useState(false);

    const {parentId, parentType, workflow} = useWorkflowDataStore();

    const {edges, nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
        }))
    );

    const {rootClusterElementNodeData} = useWorkflowEditorStore();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const queryClient = useQueryClient();

    const sourceNode = useMemo(() => nodes.find((node) => node.id === sourceNodeId), [sourceNodeId, nodes]);

    const handleActionPanelClose = useCallback(() => {
        setActionPanelOpen(false);

        setComponentDefinitionToBeAdded(null);
    }, []);

    const rootClusterElementComponentVersion =
        Number(rootClusterElementNodeData?.type?.split('/')[1].replace(/^v/, '')) || 1;

    const rootClusterElementComponentName = rootClusterElementNodeData?.componentName || '';

    const {data: rootClusterElementDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: rootClusterElementComponentName,
            componentVersion: rootClusterElementComponentVersion,
        },
        !!rootClusterElementNodeData
    );

    const handleComponentClick = useCallback(
        async (clickedItem: ClickedDefinitionType) => {
            if (!parentId || !parentType) {
                console.error(`Parent "${parentId}" of type "${parentType}" not found.`);

                return <></>;
            }

            const {clusterElement, componentVersion, name, taskDispatcher, trigger, version} = clickedItem;

            if (taskDispatcher) {
                const edge = edges.find((edge) => edge.id === edgeId);

                const taskDispatcherContext = getTaskDispatcherContext({
                    edge: edge,
                    node: edge?.type === 'workflow' ? undefined : sourceNode,
                    nodes: nodes,
                });

                await handleTaskDispatcherClick({
                    edge,
                    parentId,
                    parentType,
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

            if (
                clusterElement &&
                (!('actionsCount' in clickedItem) || !clickedItem.actionsCount) &&
                rootClusterElementDefinition
            ) {
                if (parentId && clickedItem) {
                    const getClusterElementDefinitionRequest = {
                        clusterElementName: convertNameToCamelCase(clickedItem.type),
                        componentName: clickedItem.componentName || '',
                        componentVersion: clickedItem.componentVersion || 1,
                    };

                    const clickedClusterElementDefinition = await queryClient.fetchQuery({
                        queryFn: () =>
                            new ClusterElementDefinitionApi().getComponentClusterElementDefinition(
                                getClusterElementDefinitionRequest
                            ),
                        queryKey: ClusterElementDefinitionKeys.clusterElementDefinition(
                            getClusterElementDefinitionRequest
                        ),
                    });
                    const clusterData = {
                        ...clickedItem,
                        componentName: clickedItem.componentName,
                    } as ClusterElementDefinitionBasic;

                    handleClusterElementClick({
                        clickedClusterElementDefinition,
                        data: clusterData,
                        parentId,
                        parentType,
                        queryClient,
                        rootClusterElementDefinition,
                        setPopoverOpen,
                        updateWorkflowMutation,
                    });
                }

                setPopoverOpen(false);

                return;
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

    if (!parentId || !parentType) {
        console.error(`Parent "${parentId}" of type "${parentType}" not found.`);

        return <></>;
    }

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
                    <WorkflowNodesPopoverMenuComponentList
                        actionPanelOpen={actionPanelOpen}
                        edgeId={edgeId}
                        handleComponentClick={handleComponentClick}
                        hideActionComponents={hideActionComponents}
                        hideClusterElementComponents={hideClusterElementComponents}
                        hideTaskDispatchers={hideTaskDispatchers}
                        hideTriggerComponents={hideTriggerComponents}
                        selectedComponentName={componentDefinitionToBeAdded?.name}
                        sourceData={sourceData}
                        sourceNodeId={sourceNodeId}
                    />

                    {actionPanelOpen && componentDefinitionToBeAdded && (
                        <WorkflowNodesPopoverMenuOperationList
                            clusterElementType={clusterElementType}
                            componentDefinition={componentDefinitionToBeAdded}
                            edgeId={edgeId}
                            parentId={parentId}
                            parentType={parentType}
                            rootClusterElementDefinition={rootClusterElementDefinition}
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
