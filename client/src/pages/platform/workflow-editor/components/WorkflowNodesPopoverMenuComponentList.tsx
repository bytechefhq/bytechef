import {Input} from '@/components/ui/input';
import WorkflowNodesTabs from '@/pages/platform/workflow-editor/components/workflow-nodes-tabs/WorkflowNodesTabs';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ClickedDefinitionType, NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {memo, useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {convertNameToSnakeCase} from '../../cluster-element-editor/utils/clusterElementsUtils';

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
        const [filter, setFilter] = useState('');
        const [filteredActionComponentDefinitions, setFilteredActionComponentDefinitions] = useState<
            Array<ComponentDefinitionBasic>
        >([]);

        const [filteredTaskDispatcherDefinitions, setFilteredTaskDispatcherDefinitions] = useState<
            Array<TaskDispatcherDefinition>
        >([]);

        const [filteredTriggerComponentDefinitions, setFilteredTriggerComponentDefinitions] = useState<
            Array<ComponentDefinitionBasic>
        >([]);

        const [filteredClusterElementComponentDefinitions, setFilteredClusterElementComponentDefinitions] = useState<
            Array<ComponentDefinitionBasic>
        >([]);

        const {componentDefinitions, taskDispatcherDefinitions} = useWorkflowDataStore(
            useShallow((state) => ({
                componentDefinitions: state.componentDefinitions,
                taskDispatcherDefinitions: state.taskDispatcherDefinitions,
            }))
        );
        const {nodes} = useWorkflowDataStore(useShallow((state) => ({nodes: state.nodes})));

        const ff_797 = useFeatureFlagsStore()('ff-797');
        const ff_1652 = useFeatureFlagsStore()('ff-1652');

        useEffect(
            () =>
                setFilteredTaskDispatcherDefinitions(
                    filterTaskDispatcherDefinitions(taskDispatcherDefinitions, filter, edgeId, sourceNodeId, nodes)
                ),
            [taskDispatcherDefinitions, filter, sourceNodeId, edgeId, nodes]
        );

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
                        .filter(
                            ({name}) =>
                                ((!ff_797 && name !== 'dataStream') || ff_797) &&
                                ((!ff_1652 && name !== 'aiAgent') || ff_1652)
                        )
                );

                setFilteredTriggerComponentDefinitions(
                    componentDefinitions.filter(
                        ({name, title, triggersCount}) =>
                            triggersCount &&
                            (name?.toLowerCase().includes(filter.toLowerCase()) ||
                                title?.toLowerCase().includes(filter.toLowerCase()))
                    )
                );

                if (clusterElementType) {
                    setFilteredClusterElementComponentDefinitions(
                        componentDefinitions.filter(
                            ({clusterElementsCount, name, title}) =>
                                clusterElementsCount?.[convertNameToSnakeCase(clusterElementType as string)] &&
                                (name?.toLowerCase().includes(filter.toLowerCase()) ||
                                    title?.toLowerCase().includes(filter.toLowerCase()))
                        )
                    );
                }
            }
        }, [componentDefinitions, filter, ff_797, ff_1652, clusterElementType]);

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

                    <CopilotButton parameters={{edgeId}} source={Source.WORKFLOW_EDITOR_COMPONENTS_POPOVER_MENU} />
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

            // If using edgeId (has full data), or using sourceNodeId (has restricted data)
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
