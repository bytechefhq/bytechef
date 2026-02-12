import {Input} from '@/components/ui/input';
import WorkflowNodesTabs from '@/pages/platform/workflow-editor/components/workflow-nodes-tabs/WorkflowNodesTabs';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionWithActionsProps} from '@/shared/queries/platform/componentDefinitionsGraphQL.queries';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ClickedDefinitionType, NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {memo, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {convertNameToSnakeCase} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {useFilteredComponentDefinitions} from '../hooks/useFilteredComponentDefinitions';

interface WorkflowNodesListProps {
    actionPanelOpen: boolean;
    clusterElementType?: string;
    edgeId?: string;
    handleComponentClick?: (clickedItem: ClickedDefinitionType) => void;
    hideActionComponents?: boolean;
    hideClusterElementComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    selectedComponentName?: string;
    sourceNodeId?: string;
}

const hasClusterElementType = (component: ComponentDefinitionWithActionsProps, clusterElementType: string): boolean => {
    const clusterTypeSnakeCase = convertNameToSnakeCase(clusterElementType);

    const hasClusterElementsCount = component.clusterElementsCount?.[clusterTypeSnakeCase];

    const hasClusterElements = component.clusterElements?.some(
        (element) => element.type?.name === clusterTypeSnakeCase
    );

    return !!(hasClusterElementsCount || hasClusterElements);
};

const WorkflowNodesPopoverMenuComponentList = memo(
    ({
        actionPanelOpen,
        clusterElementType,
        edgeId,
        handleComponentClick,
        hideActionComponents = false,
        hideClusterElementComponents = false,
        hideTaskDispatchers = false,
        hideTriggerComponents = false,
        selectedComponentName,
        sourceNodeId,
    }: WorkflowNodesListProps) => {
        const {componentDefinitions, taskDispatcherDefinitions} = useWorkflowDataStore(
            useShallow((state) => ({
                componentDefinitions: state.componentDefinitions,
                taskDispatcherDefinitions: state.taskDispatcherDefinitions,
            }))
        );
        const {nodes} = useWorkflowDataStore(useShallow((state) => ({nodes: state.nodes})));

        const {componentsWithActions, filter, setFilter, trimmedFilter} =
            useFilteredComponentDefinitions(componentDefinitions);

        const getFeatureFlag = useFeatureFlagsStore();

        const ff_797 = getFeatureFlag('ff-797');
        const ff_1652 = getFeatureFlag('ff-1652');
        const ff_3827 = getFeatureFlag('ff-3827');
        const ff_3839 = getFeatureFlag('ff-3839');
        const ff_4000 = getFeatureFlag('ff-4000');

        const filteredActionComponentDefinitions = useMemo(() => {
            if (!componentsWithActions) {
                return [];
            }

            let actionComponents = componentsWithActions
                .filter(({actionsCount}) => actionsCount && actionsCount > 0)
                .filter(
                    ({name}) =>
                        ((!ff_797 && name !== 'dataStream') || ff_797) &&
                        ((!ff_1652 && name !== 'aiAgent') || ff_1652) &&
                        ((!ff_4000 && name !== 'knowledgeBase') || ff_4000)
                );

            if (clusterElementType) {
                actionComponents = actionComponents.filter((component) =>
                    hasClusterElementType(component, clusterElementType)
                );
            }

            return actionComponents;
        }, [componentsWithActions, clusterElementType, ff_797, ff_1652, ff_4000]);

        const filteredTaskDispatcherDefinitions = useMemo(
            () =>
                filterTaskDispatcherDefinitions(taskDispatcherDefinitions, trimmedFilter, edgeId, sourceNodeId, nodes),
            [taskDispatcherDefinitions, trimmedFilter, edgeId, sourceNodeId, nodes]
        );

        const filteredTriggerComponentDefinitions = useMemo(() => {
            if (!componentsWithActions) {
                return [];
            }

            let triggerComponents = componentsWithActions
                .filter(({triggersCount}) => triggersCount && triggersCount > 0)
                .filter(({name}) => (!ff_3827 && name !== 'form') || ff_3827);

            if (clusterElementType) {
                triggerComponents = triggerComponents.filter((component) =>
                    hasClusterElementType(component, clusterElementType)
                );
            }

            return triggerComponents;
        }, [componentsWithActions, clusterElementType, ff_3827]);

        const filteredClusterElementComponentDefinitions = useMemo(() => {
            if (!componentsWithActions || !clusterElementType) {
                return [];
            }

            return componentsWithActions
                .filter((component) => hasClusterElementType(component, clusterElementType))
                .filter(({name}) => (!ff_3839 && name !== 'aiAgent') || ff_3839);
        }, [componentsWithActions, clusterElementType, ff_3839]);

        return (
            <div className={twMerge('rounded-lg', actionPanelOpen ? 'w-node-popover-width' : 'w-full')}>
                <header className="flex items-center gap-1 rounded-t-lg px-3 pt-3 text-center">
                    <Input
                        className="bg-white shadow-none"
                        name="workflowNodeFilter"
                        onChange={(event) => setFilter(event.target.value)}
                        placeholder="Filter components"
                        value={filter}
                    />
                </header>

                <div className="h-96 rounded-b-lg pb-3">
                    <WorkflowNodesTabs
                        actionComponentDefinitions={filteredActionComponentDefinitions}
                        clusterElementComponentDefinitions={filteredClusterElementComponentDefinitions}
                        hideActionComponents={hideActionComponents}
                        hideClusterElementComponents={hideClusterElementComponents}
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

const isLoopSubtask = (node?: Node) => !!node?.data?.loopData || !!node?.data.loopId;

const isTaskDispatcherSubtask = (node?: Node) =>
    !!node?.data?.loopData ||
    !!node?.data?.conditionData ||
    !!node?.data?.branchData ||
    !!node?.data?.parallelData ||
    !!node?.data?.eachData ||
    !!node?.data?.forkJoinData;

const filterTaskDispatcherDefinitions = (
    taskDispatcherDefinitions: Array<TaskDispatcherDefinition> | null,
    filter: string,
    edgeId?: string,
    sourceNodeId?: string,
    nodes: Node[] = []
) => {
    if (!taskDispatcherDefinitions) {
        return [];
    }

    const nodeId = sourceNodeId || edgeId?.split('=>')[0];

    if (!nodeId) {
        return taskDispatcherDefinitions;
    }

    const filteredBySearch = taskDispatcherDefinitions.filter(
        ({name, title}) =>
            name?.toLowerCase().includes(filter.toLowerCase()) || title?.toLowerCase().includes(filter.toLowerCase())
    );

    const result = [...filteredBySearch];

    if (nodeId.startsWith('loop_') && nodeId.includes('placeholder')) {
        return result;
    }

    const sourceNode = nodes.find((node) => node.id === nodeId);

    if (!sourceNode) {
        return result.filter(({name}) => name !== 'loopBreak');
    }

    let hasLoopTaskDispatcher = false;

    if (isLoopSubtask(sourceNode)) {
        hasLoopTaskDispatcher = true;
    } else {
        let currentNode = sourceNode;

        while (currentNode) {
            let parentId;

            const currentNodeData = currentNode.data as NodeDataType;

            if (currentNode.data.workflowNodeName) {
                parentId = currentNodeData.conditionData?.conditionId || currentNodeData.branchData?.branchId;
            } else {
                parentId = currentNodeData.conditionId || currentNodeData.branchId;
            }

            const parentNode = nodes.find((node) => node.id === parentId);

            if (!parentNode || !isTaskDispatcherSubtask(parentNode)) {
                break;
            }

            if (isLoopSubtask(parentNode)) {
                hasLoopTaskDispatcher = true;
                break;
            }

            currentNode = parentNode;
        }
    }

    if (!hasLoopTaskDispatcher) {
        return result.filter(({name}) => name !== 'loopBreak');
    }

    return result;
};
