import {Input} from '@/components/ui/input';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {
    ComponentDefinitionWithActionsProps,
    useGetComponentDefinitionsWithActionsQuery,
} from '@/shared/queries/platform/componentDefinitionsGraphQL.queries';
import {useEffect, useMemo, useState} from 'react';
import {useDebounce} from 'use-debounce';

import WorkflowNodesTabs from './workflow-nodes-tabs/WorkflowNodesTabs';

const WorkflowNodesSidebar = ({
    data,
}: {
    data: {
        componentDefinitions: Array<ComponentDefinitionBasic>;
        taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
    };
}) => {
    const [filter, setFilter] = useState('');
    const [debouncedFilter] = useDebounce(filter, 300);

    const [filteredActionComponentDefinitions, setFilteredActionComponentDefinitions] = useState<
        Array<ComponentDefinitionBasic | ComponentDefinitionWithActionsProps>
    >([]);

    const [filteredTaskDispatcherDefinitions, setFilteredTaskDispatcherDefinitions] = useState<
        Array<TaskDispatcherDefinition>
    >([]);

    const [filteredTriggerComponentDefinitions, setFilteredTriggerComponentDefinitions] = useState<
        Array<ComponentDefinitionBasic | ComponentDefinitionWithActionsProps>
    >([]);

    const {componentDefinitions, taskDispatcherDefinitions} = data;

    const trimmedFilter = debouncedFilter.trim();

    const {data: searchedComponentDefinitions, isLoading: isSearchLoading} =
        useGetComponentDefinitionsWithActionsQuery(trimmedFilter);

    const componentsWithActions = useMemo(() => {
        if (trimmedFilter && searchedComponentDefinitions && !isSearchLoading) {
            return searchedComponentDefinitions;
        }

        return componentDefinitions;
    }, [trimmedFilter, searchedComponentDefinitions, isSearchLoading, componentDefinitions]);

    useEffect(() => {
        setFilteredActionComponentDefinitions(
            componentsWithActions.filter(
                (componentDefinition) => componentDefinition?.actionsCount && componentDefinition.actionsCount > 0
            )
        );

        setFilteredTaskDispatcherDefinitions(
            taskDispatcherDefinitions.filter(
                (taskDispatcherDefinition) =>
                    taskDispatcherDefinition.name?.toLowerCase().includes(trimmedFilter.toLowerCase()) ||
                    taskDispatcherDefinition?.title?.toLowerCase().includes(trimmedFilter.toLowerCase())
            )
        );

        setFilteredTriggerComponentDefinitions(
            componentsWithActions.filter(
                (componentDefinition) => componentDefinition?.triggersCount && componentDefinition.triggersCount > 0
            )
        );
    }, [componentsWithActions, trimmedFilter, taskDispatcherDefinitions]);

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
