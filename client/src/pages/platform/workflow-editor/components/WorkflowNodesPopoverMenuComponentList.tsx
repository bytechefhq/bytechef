import {Input} from '@/components/ui/input';
import WorkflowNodesTabs from '@/pages/platform/workflow-editor/components/WorkflowNodesTabs';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ClickedDefinitionType} from '@/shared/types';
import {memo, useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface WorkflowNodesListProps {
    actionPanelOpen: boolean;
    handleComponentClick?: (clickedItem: ClickedDefinitionType) => void;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    id: string;
    selectedComponentName?: string;
}

const WorkflowNodesPopoverMenuComponentList = memo(
    ({
        actionPanelOpen,
        handleComponentClick,
        hideActionComponents = false,
        hideTaskDispatchers = false,
        hideTriggerComponents = false,
        selectedComponentName,
    }: WorkflowNodesListProps) => {
        const [filter, setFilter] = useState('');

        const ff_797 = useFeatureFlagsStore()('ff-797');

        const [filteredActionComponentDefinitions, setFilteredActionComponentDefinitions] = useState<
            Array<ComponentDefinitionBasic>
        >([]);

        const [filteredTaskDispatcherDefinitions, setFilteredTaskDispatcherDefinitions] = useState<
            Array<TaskDispatcherDefinition>
        >([]);

        const [filteredTriggerComponentDefinitions, setFilteredTriggerComponentDefinitions] = useState<
            Array<ComponentDefinitionBasic>
        >([]);

        const {componentDefinitions, taskDispatcherDefinitions} = useWorkflowDataStore();

        useEffect(() => {
            if (taskDispatcherDefinitions) {
                setFilteredTaskDispatcherDefinitions(
                    taskDispatcherDefinitions.filter(
                        ({name, title}) =>
                            name?.toLowerCase().includes(filter.toLowerCase()) ||
                            title?.toLowerCase().includes(filter.toLowerCase())
                    )
                );
            }
        }, [taskDispatcherDefinitions, filter]);

        useEffect(() => {
            if (componentDefinitions) {
                setFilteredActionComponentDefinitions(
                    componentDefinitions
                        .filter(
                            ({actionsCount, name, title}) =>
                                actionsCount &&
                                (name?.toLowerCase().includes(filter.toLowerCase()) ||
                                    title?.toLowerCase().includes(filter.toLowerCase()))
                        )
                        .filter(({name}) => (!ff_797 && name !== 'dataStream') || ff_797)
                );

                setFilteredTriggerComponentDefinitions(
                    componentDefinitions.filter(
                        ({name, title, triggersCount}) =>
                            triggersCount &&
                            (name?.toLowerCase().includes(filter.toLowerCase()) ||
                                title?.toLowerCase().includes(filter.toLowerCase()))
                    )
                );
            }
        }, [componentDefinitions, filter, ff_797]);

        return (
            <div
                className={twMerge(
                    'rounded-lg',
                    actionPanelOpen ? 'w-workflow-nodes-popover-component-menu-width' : 'w-full'
                )}
            >
                <header className="rounded-tl-lg bg-white p-3 text-center">
                    <Input
                        name="workflowNodeFilter"
                        onChange={(event) => setFilter(event.target.value)}
                        placeholder="Filter components, triggers and flows"
                        value={filter}
                    />
                </header>

                <div className="h-96 rounded-bl-lg pb-3">
                    <WorkflowNodesTabs
                        actionComponentDefinitions={filteredActionComponentDefinitions}
                        hideActionComponents={hideActionComponents}
                        hideTaskDispatchers={hideTaskDispatchers}
                        hideTriggerComponents={hideTriggerComponents}
                        onItemClick={handleComponentClick}
                        selectedComponentName={selectedComponentName}
                        taskDispatcherDefinitions={filteredTaskDispatcherDefinitions}
                        triggerComponentDefinitions={filteredTriggerComponentDefinitions}
                    />
                </div>
            </div>
        );
    }
);

WorkflowNodesPopoverMenuComponentList.displayName = 'WorkflowNodesPopoverMenuList';

export default WorkflowNodesPopoverMenuComponentList;
