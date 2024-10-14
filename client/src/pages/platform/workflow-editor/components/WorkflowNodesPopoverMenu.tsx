import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ComponentDefinition, ComponentDefinitionApi} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {ClickedDefinitionType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {PropsWithChildren, useEffect, useState} from 'react';
import {useReactFlow} from 'reactflow';
import {twMerge} from 'tailwind-merge';

import {useWorkflowMutation} from '../providers/workflowMutationProvider';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import handleConditionClick from '../utils/handleConditionClick';
import WorkflowNodesPopoverMenuComponentList from './WorkflowNodesPopoverMenuComponentList';
import WorkflowNodesPopoverMenuOperationList from './WorkflowNodesPopoverMenuOperationList';

interface WorkflowNodesPopoverMenuProps extends PropsWithChildren {
    condition?: boolean;
    sourceNodeId: string;
    edge?: boolean;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
}

const WorkflowNodesPopoverMenu = ({
    children,
    condition = false,
    edge = false,
    hideActionComponents = false,
    hideTaskDispatchers = false,
    hideTriggerComponents = false,
    sourceNodeId,
}: WorkflowNodesPopoverMenuProps) => {
    const [actionPanelOpen, setActionPanelOpen] = useState(false);
    const [componentDefinitionToBeAdded, setComponentDefinitionToBeAdded] = useState<ComponentDefinition | null>(null);
    const [trigger, setTrigger] = useState(false);

    const {componentDefinitions, setWorkflow, taskDispatcherDefinitions, workflow} = useWorkflowDataStore();

    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const {getNode, setNodes} = useReactFlow();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const queryClient = useQueryClient();

    const handleActionPanelClose = () => {
        setActionPanelOpen(false);

        setComponentDefinitionToBeAdded(null);
    };

    const handleComponentClick = async (clickedItem: ClickedDefinitionType) => {
        if (clickedItem.name.includes('condition')) {
            await handleConditionClick({
                clickedItem,
                currentNode,
                edge,
                getNode,
                queryClient,
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                setNodes: setNodes as any,
                setWorkflow,
                sourceNodeId,
                updateWorkflowMutation,
                workflow,
            });

            return;
        }

        if (clickedItem.trigger) {
            setTrigger(true);
        }

        const clickedComponentDefinition = await queryClient.fetchQuery({
            queryFn: () =>
                new ComponentDefinitionApi().getComponentDefinition({
                    componentName: clickedItem.name,
                }),
            queryKey: ComponentDefinitionKeys.componentDefinition({
                componentName: clickedItem.name,
            }),
        });

        if (clickedComponentDefinition) {
            setComponentDefinitionToBeAdded(clickedComponentDefinition);
        }
    };

    useEffect(() => {
        if (componentDefinitionToBeAdded) {
            setActionPanelOpen(true);
        }
    }, [componentDefinitionToBeAdded]);

    return (
        <Popover onOpenChange={(open) => !open && handleActionPanelClose()}>
            <PopoverTrigger asChild>{children}</PopoverTrigger>

            <PopoverContent
                align="start"
                className={twMerge(
                    'flex p-0 will-change-auto rounded-lg',
                    actionPanelOpen
                        ? 'w-workflow-nodes-popover-menu-width'
                        : 'w-workflow-nodes-popover-component-menu-width'
                )}
                side="right"
                sideOffset={-34}
            >
                <div className="nowheel flex w-full rounded-lg bg-gray-100">
                    {typeof componentDefinitions === 'undefined' ||
                        (typeof taskDispatcherDefinitions === 'undefined' && (
                            <div className="px-3 py-2 text-xs">Something went wrong.</div>
                        ))}

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
                            condition={condition}
                            edge={edge}
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
