import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {
    ComponentDefinition,
    TaskDispatcherDefinition,
    TriggerDefinition,
    WorkflowTask,
} from '@/shared/middleware/platform/configuration';
import {NodeDataType, UpdateWorkflowMutationType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {ChangeEvent} from 'react';
import {useParams} from 'react-router-dom';
import {useDebouncedCallback} from 'use-debounce';
import {useShallow} from 'zustand/react/shallow';

import saveWorkflowDefinition from '../../utils/saveWorkflowDefinition';
import updateRootConditionNode from '../../utils/updateRootConditionNode';
import updateRootLoopNode from '../../utils/updateRootLoopNode';

const DescriptionTab = ({
    nodeDefinition,
    updateWorkflowMutation,
}: {
    nodeDefinition: ComponentDefinition | TaskDispatcherDefinition | TriggerDefinition;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}) => {
    const {workflow} = useWorkflowDataStore();
    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore();

    const queryClient = useQueryClient();

    const {projectId} = useParams();

    const updateNodeData = (value: string, field: 'label' | 'description'): NodeDataType | undefined => {
        if (!currentNode || !workflow.definition) {
            return;
        }

        let nodeData = {
            ...currentNode!,
            [field]: value,
            name: currentNode.workflowNodeName,
        };

        const tasks = JSON.parse(workflow.definition).tasks;

        if (currentNode.conditionData) {
            const parentConditionNode = nodes.find(
                (node) => node.data.name === currentNode?.conditionData?.conditionId
            );

            if (!parentConditionNode) {
                return;
            }

            const conditionCase = currentNode.conditionData.conditionCase;
            const conditionParameters: Array<WorkflowTask> = (parentConditionNode.data as NodeDataType)?.parameters?.[
                conditionCase
            ];

            if (!conditionParameters) {
                return;
            }

            const taskIndex = conditionParameters.findIndex((subtask) => subtask.name === currentNode.name);

            if (taskIndex === -1) {
                return;
            }

            conditionParameters[taskIndex] = {
                ...conditionParameters[taskIndex],
                [field]: value,
            };

            const updatedParentConditionTask = workflow.tasks?.find(
                (task) => task.name === currentNode.conditionData?.conditionId
            );

            if (!updatedParentConditionTask) {
                return;
            }

            nodeData = updateRootConditionNode({
                conditionCase,
                conditionId: currentNode.conditionData.conditionId,
                nodeIndex: taskIndex,
                nodes,
                tasks,
                updatedParentConditionNodeData: parentConditionNode.data as NodeDataType,
                updatedParentConditionTask,
                workflow,
            });
        }

        if (currentNode.loopData) {
            const parentLoopNode = nodes.find((node) => node.data.name === currentNode?.loopData?.loopId);

            if (!parentLoopNode) {
                return;
            }

            const loopIteratees: Array<WorkflowTask> = (parentLoopNode.data as NodeDataType)?.parameters?.iteratee;

            const taskIndex = loopIteratees.findIndex((subtask) => subtask.name === currentNode.name);

            if (taskIndex === -1) {
                return;
            }

            loopIteratees[taskIndex] = {
                ...loopIteratees[taskIndex],
                [field]: value,
            };

            const updatedParentLoopTask = workflow.tasks?.find((task) => task.name === currentNode.loopData?.loopId);

            if (!updatedParentLoopTask) {
                return;
            }

            nodeData = updateRootLoopNode({
                loopId: currentNode.loopData.loopId,
                nodeIndex: taskIndex,
                nodes,
                tasks,
                updatedParentLoopNodeData: parentLoopNode.data as NodeDataType,
                updatedParentLoopTask,
                workflow,
            });
        }

        return nodeData;
    };

    const handleLabelChange = useDebouncedCallback((event: ChangeEvent<HTMLInputElement>) => {
        if (!currentNode) {
            return;
        }

        let nodeData: NodeDataType = {
            ...currentNode,
            label: event.target.value,
            name: currentNode.workflowNodeName,
            version: 'version' in nodeDefinition ? nodeDefinition.version : 1,
        };

        if (currentNode.conditionData || currentNode.loopData) {
            nodeData = updateNodeData(event.target.value, 'label') ?? nodeData;
        }

        saveWorkflowDefinition({
            decorative: true,
            nodeData,
            onSuccess: () => {
                setCurrentComponent({
                    ...currentComponent,
                    componentName: currentNode.componentName,
                    label: event.target.value,
                    workflowNodeName: currentNode.workflowNodeName,
                });

                setCurrentNode({
                    ...currentNode,
                    label: event.target.value,
                });
            },
            projectId: +projectId!,
            queryClient,
            updateWorkflowMutation,
        });
    }, 200);

    const handleNotesChange = useDebouncedCallback((event: ChangeEvent<HTMLTextAreaElement>) => {
        if (!currentNode) {
            return;
        }

        let nodeData: NodeDataType = {
            ...currentNode,
            description: event.target.value,
            name: currentNode.workflowNodeName,
            version: 'version' in nodeDefinition ? nodeDefinition.version : 1,
        };

        if (currentNode.conditionData || currentNode.loopData) {
            nodeData = updateNodeData(event.target.value, 'description') ?? nodeData;
        }

        saveWorkflowDefinition({
            decorative: true,
            nodeData,
            onSuccess: () => {
                setCurrentComponent({
                    ...currentComponent,
                    componentName: currentNode.componentName,
                    description: event.target.value,
                    workflowNodeName: currentNode.workflowNodeName,
                });

                setCurrentNode({
                    ...currentNode,
                    description: event.target.value,
                });
            },
            projectId: +projectId!,
            queryClient,
            updateWorkflowMutation,
        });
    }, 200);

    return (
        <div className="flex h-full flex-col gap-4 overflow-auto p-4">
            <fieldset className="space-y-1">
                <Label>Title</Label>

                <Input
                    defaultValue={currentNode?.label}
                    key={`${currentNode?.componentName}_nodeTitle`}
                    name="nodeTitle"
                    onChange={handleLabelChange}
                />
            </fieldset>

            <fieldset className="space-y-1">
                <Label>Notes</Label>

                <Textarea
                    defaultValue={currentNode?.description || ''}
                    key={`${currentNode?.componentName}_nodeNotes`}
                    name="nodeNotes"
                    onChange={handleNotesChange}
                    placeholder="Write some notes for yourself..."
                    rows={6}
                />
            </fieldset>
        </div>
    );
};

export default DescriptionTab;
