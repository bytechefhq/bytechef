import {Input} from '@/components/ui/input';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

import {useFilteredComponentDefinitions} from '../hooks/useFilteredComponentDefinitions';
import WorkflowNodesTabs from './workflow-nodes-tabs/WorkflowNodesTabs';

const WorkflowNodesSidebar = ({
    data,
    visible,
}: {
    data: {
        componentDefinitions: Array<ComponentDefinitionBasic>;
        taskDispatcherDefinitions: Array<TaskDispatcherDefinition>;
    };
    visible: boolean;
}) => {
    const {componentsWithActions, filter, setFilter, trimmedFilter} = useFilteredComponentDefinitions(
        data.componentDefinitions
    );

    const getFeatureFlag = useFeatureFlagsStore();

    const ff_732 = getFeatureFlag('ff-732');
    const ff_797 = getFeatureFlag('ff-797');
    const ff_4000 = getFeatureFlag('ff-4000');

    const knowledgeBaseEnabled = useApplicationInfoStore((state) => state.ai.knowledgeBase.enabled);

    const filteredActionComponentDefinitions = useMemo(() => {
        if (!componentsWithActions) {
            return [];
        }

        const actionComponents = componentsWithActions
            .filter(({actionsCount}) => actionsCount && actionsCount > 0)
            .filter(
                ({name}) =>
                    ((!ff_732 && name !== 'approval') || ff_732) &&
                    ((!ff_797 && name !== 'dataStream') || ff_797) &&
                    (((!ff_4000 || !knowledgeBaseEnabled) && name !== 'knowledgeBase') ||
                        (ff_4000 && knowledgeBaseEnabled))
            );

        return actionComponents;
    }, [componentsWithActions, ff_4000, ff_732, ff_797, knowledgeBaseEnabled]);

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
        <aside
            className={twMerge(
                'absolute inset-y-2 right-14 flex w-96 flex-col overflow-hidden rounded-md border border-stroke-neutral-secondary bg-surface-neutral-secondary pb-4 transition-[transform,opacity] duration-300 ease-in-out',
                visible ? 'translate-x-0 opacity-100' : 'translate-x-4 opacity-0'
            )}
        >
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
