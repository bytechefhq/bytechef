import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ComponentDefinition, ComponentDefinitionApi} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {ClickedDefinitionType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {PropsWithChildren, useCallback, useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {useWorkflowMutation} from '../providers/workflowMutationProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import handleConditionClick from '../utils/handleConditionClick';
import handleLoopClick from '../utils/handleLoopClick';
import WorkflowNodesPopoverMenuComponentList from './WorkflowNodesPopoverMenuComponentList';
import WorkflowNodesPopoverMenuOperationList from './WorkflowNodesPopoverMenuOperationList';

interface WorkflowNodesPopoverMenuProps extends PropsWithChildren {
    conditionId?: string;
    edge?: boolean;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    loopId?: string;
    nodeIndex?: number;
    sourceNodeId: string;
}

const WorkflowNodesPopoverMenu = ({
    children,
    conditionId,
    edge = false,
    hideActionComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    loopId,
    nodeIndex,
    sourceNodeId,
}: WorkflowNodesPopoverMenuProps) => {
    const [actionPanelOpen, setActionPanelOpen] = useState(false);
    const [componentDefinitionToBeAdded, setComponentDefinitionToBeAdded] = useState<ComponentDefinition | null>(null);
    const [popoverOpen, setPopoverOpen] = useState(false);
    const [trigger, setTrigger] = useState(false);

    const {componentDefinitions, taskDispatcherDefinitions, workflow} = useWorkflowDataStore();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const queryClient = useQueryClient();

    const memoizedComponentDefinitions = useMemo(() => componentDefinitions, [componentDefinitions]);
    const memoizedTaskDispatcherDefinitions = useMemo(() => taskDispatcherDefinitions, [taskDispatcherDefinitions]);

    const handleActionPanelClose = useCallback(() => {
        setActionPanelOpen(false);

        setComponentDefinitionToBeAdded(null);
    }, []);

    const handleComponentClick = useCallback(
        async (clickedItem: ClickedDefinitionType) => {
            if (clickedItem.taskDispatcher) {
                if (clickedItem.name.includes('condition')) {
                    await handleConditionClick({
                        clickedItem,
                        edge,
                        queryClient,
                        sourceNodeId,
                        updateWorkflowMutation,
                        workflow,
                    });
                } else if (clickedItem.name.includes('loop')) {
                    await handleLoopClick({
                        clickedItem,
                        edge,
                        queryClient,
                        sourceNodeId,
                        updateWorkflowMutation,
                        workflow,
                    });
                }

                setPopoverOpen(false);

                return;
            }

            if (clickedItem.trigger) {
                setTrigger(true);
            }

            const clickedComponentDefinition = await queryClient.fetchQuery({
                queryFn: () =>
                    new ComponentDefinitionApi().getComponentDefinition({
                        componentName: clickedItem.name,
                        componentVersion: clickedItem.componentVersion || clickedItem.version,
                    }),
                queryKey: ComponentDefinitionKeys.componentDefinition({
                    componentName: clickedItem.name,
                    componentVersion: clickedItem.componentVersion || clickedItem.version,
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
            <PopoverTrigger asChild>{children}</PopoverTrigger>

            <PopoverContent
                align="start"
                className={twMerge(
                    'flex rounded-lg p-0 will-change-auto',
                    actionPanelOpen
                        ? 'w-workflow-nodes-popover-menu-width'
                        : 'w-workflow-nodes-popover-component-menu-width'
                )}
                side="right"
                sideOffset={-34}
            >
                <div className="nowheel flex w-full rounded-lg bg-gray-100">
                    {(!memoizedComponentDefinitions || !memoizedTaskDispatcherDefinitions) && (
                        <div className="px-3 py-2 text-xs">Something went wrong.</div>
                    )}

                    <WorkflowNodesPopoverMenuComponentList
                        actionPanelOpen={actionPanelOpen}
                        handleComponentClick={handleComponentClick}
                        hideActionComponents={hideActionComponents}
                        hideTaskDispatchers={hideTaskDispatchers}
                        hideTriggerComponents={hideTriggerComponents}
                        selectedComponentName={componentDefinitionToBeAdded?.name}
                        sourceNodeId={sourceNodeId}
                    />

                    {actionPanelOpen && componentDefinitionToBeAdded && (
                        <WorkflowNodesPopoverMenuOperationList
                            componentDefinition={componentDefinitionToBeAdded}
                            conditionId={conditionId}
                            edge={edge}
                            loopId={loopId}
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
