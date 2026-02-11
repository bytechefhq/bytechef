import {Input} from '@/components/ui/input';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';

import {useFilteredComponentDefinitions} from '../hooks/useFilteredComponentDefinitions';
import WorkflowNodesTabs from './workflow-nodes-tabs/WorkflowNodesTabs';

const WorkflowNodesSidebar = ({
    data,
}: {
    data: {
        componentDefinitions: Array<ComponentDefinitionBasic>;
        taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
    };
}) => {
    const {componentsWithActions, filter, setFilter, trimmedFilter} = useFilteredComponentDefinitions(
        data.componentDefinitions
    );

    const filteredActionComponentDefinitions = useMemo(
        () =>
            componentsWithActions.filter(
                (componentDefinition) => componentDefinition?.actionsCount && componentDefinition.actionsCount > 0
            ),
        [componentsWithActions]
    );

    const filteredTaskDispatcherDefinitions = useMemo(
        () =>
            data.taskDispatcherDefinitions.filter(
                (taskDispatcherDefinition) =>
                    taskDispatcherDefinition.name?.toLowerCase().includes(trimmedFilter.toLowerCase()) ||
                    taskDispatcherDefinition?.title?.toLowerCase().includes(trimmedFilter.toLowerCase())
            ),
        [data.taskDispatcherDefinitions, trimmedFilter]
    );

    const filteredTriggerComponentDefinitions = useMemo(
        () =>
            componentsWithActions.filter(
                (componentDefinition) => componentDefinition?.triggersCount && componentDefinition.triggersCount > 0
            ),
        [componentsWithActions]
    );

    return (
        <aside className="absolute inset-y-2 right-14 flex w-96 flex-col overflow-hidden rounded-md border border-stroke-neutral-secondary bg-surface-neutral-secondary pb-4">
            <div className="px-3 pt-3 text-center text-content-neutral-secondary">
                <Input
                    className="bg-white shadow-none"
                    name="workflowNodeFilter"
                    onChange={(event) => setFilter(event.target.value)}
                    placeholder="Filter components"
                    value={filter}
                />
            </div>

            <div className="flex flex-1 flex-col overflow-hidden pt-1">
                <WorkflowNodesTabs
                    actionComponentDefinitions={filteredActionComponentDefinitions}
                    hideClusterElementComponents
                    itemsDraggable
                    taskDispatcherDefinitions={filteredTaskDispatcherDefinitions}
                    triggerComponentDefinitions={filteredTriggerComponentDefinitions}
                />
            </div>
        </aside>
    );
};

export default WorkflowNodesSidebar;
