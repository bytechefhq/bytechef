import {Input} from '@/components/ui/input';
import WorkflowNodesTabs from '@/pages/platform/workflow-editor/components/WorkflowNodesTabs';
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

interface WorkflowNodesListProps {
    actionPanelOpen: boolean;
    edgeId?: string;
    handleComponentClick?: (clickedItem: ClickedDefinitionType) => void;
    hideActionComponents?: boolean;
    hideTriggerComponents?: boolean;
    hideTaskDispatchers?: boolean;
    selectedComponentName?: string;
    sourceNodeId?: string;
}

const WorkflowNodesPopoverMenuComponentList = memo(
    ({
        actionPanelOpen,
        edgeId,
        handleComponentClick,
        hideActionComponents = false,
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

        const {componentDefinitions, taskDispatcherDefinitions} = useWorkflowDataStore();

        const {nodes} = useWorkflowDataStore(useShallow((state) => ({nodes: state.nodes})));

        const ff_797 = useFeatureFlagsStore()('ff-797');

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
            <div className={twMerge('rounded-lg', actionPanelOpen ? 'w-node-popover-width' : 'w-full')}>
                <header className="flex items-center gap-1 rounded-t-lg bg-background p-3 text-center">
                    <Input
                        name="workflowNodeFilter"
                        onChange={(event) => setFilter(event.target.value)}
                        placeholder="Filter actions, triggers and flows"
                        value={filter}
                    />

                    <CopilotButton parameters={{edgeId}} source={Source.WORKFLOW_EDITOR_COMPONENTS_POPOVER_MENU} />
                </header>

                <div className="h-96 rounded-b-lg pb-3">
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

    const filteredBySearch = taskDispatcherDefinitions.filter(
        ({name, title}) =>
            name?.toLowerCase().includes(filter.toLowerCase()) || title?.toLowerCase().includes(filter.toLowerCase())
    );

    const result = [...filteredBySearch];

    if (edgeId) {
        const [sourceNodeId, targetNodeId] = edgeId.split('=>');
        const isSourceTopLoopGhostNode = sourceNodeId.startsWith('loop_') && sourceNodeId.includes('ghost');
        const isTargetBottomLoopGhostNode = targetNodeId.startsWith('loop_') && targetNodeId.includes('ghost');

        if (isSourceTopLoopGhostNode || isTargetBottomLoopGhostNode) {
            return result;
        }

        const sourceNode = nodes.find((node) => node.id === sourceNodeId);
        const targetNode = nodes.find((node) => node.id === targetNodeId);

        if ((sourceNode?.data as NodeDataType).loopData || (targetNode?.data as NodeDataType).loopData) {
            return result;
        }
    }

    if (!edgeId?.startsWith('loop_') && !sourceNodeId?.startsWith('loop_')) {
        return result.filter(({name}) => name !== 'loopBreak');
    }

    return result;
};
