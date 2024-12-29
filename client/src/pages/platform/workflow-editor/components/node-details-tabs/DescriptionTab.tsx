import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {WorkflowTask} from '@/shared/middleware/automation/configuration';
import {ComponentType, NodeDataType, UpdateWorkflowMutationType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {ChangeEvent} from 'react';
import {useDebouncedCallback} from 'use-debounce';

import saveWorkflowDefinition from '../../utils/saveWorkflowDefinition';
import updateRootConditionNode from '../../utils/updateRootConditionNode';

const DescriptionTab = ({updateWorkflowMutation}: {updateWorkflowMutation: UpdateWorkflowMutationType}) => {
    const {nodes, workflow} = useWorkflowDataStore();
    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore();

    const queryClient = useQueryClient();

    const componentData: ComponentType = {
        ...currentComponent!,
        workflowNodeName: currentNode!.workflowNodeName,
    };

    const updateNodeData = (value: string, field: 'label' | 'description'): NodeDataType | undefined => {
        if (!currentComponent || !currentNode) {
            return;
        }

        let nodeData = {
            ...currentComponent!,
            [field]: value,
            name: currentComponent.workflowNodeName,
        };

        if (currentNode.conditionData) {
            const parentConditionNode = nodes.find(
                (node) => node.data.name === currentNode?.conditionData?.conditionId
            );

            if (!parentConditionNode) {
                return;
            }

            const conditionCase = currentNode.conditionData.conditionCase;
            const conditionParameters: Array<WorkflowTask> = parentConditionNode.data.parameters[conditionCase];

            if (conditionParameters) {
                const taskIndex = conditionParameters.findIndex((subtask) => subtask.name === currentNode.name);

                if (taskIndex !== -1) {
                    conditionParameters[taskIndex] = {
                        ...conditionParameters[taskIndex],
                        [field]: value,
                    };

                    if (!workflow.definition) {
                        return;
                    }

                    const tasks = JSON.parse(workflow.definition).tasks;

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
                        updatedParentConditionNodeData: parentConditionNode.data,
                        updatedParentConditionTask,
                        workflow,
                    });

                    console.log('nodeData', nodeData);
                }
            }
        }

        return nodeData;
    };

    const handleLabelChange = useDebouncedCallback((event: ChangeEvent<HTMLInputElement>) => {
        if (!currentComponent || !currentNode) {
            return;
        }

        let nodeData: NodeDataType = {
            ...currentComponent!,
            label: event.target.value,
            name: currentComponent.workflowNodeName,
        };

        if (currentNode.conditionData) {
            nodeData = updateNodeData(event.target.value, 'label') ?? nodeData;
        }

        console.log('currentNode before save: ', currentNode);
        saveWorkflowDefinition({
            decorative: true,
            nodeData,
            onSuccess: () => {
                setCurrentComponent({
                    ...currentComponent,
                    label: event.target.value,
                });

                setCurrentNode({
                    ...currentNode,
                    label: event.target.value,
                });
            },
            queryClient,
            updateWorkflowMutation,
            workflow,
        });
    }, 200);

    const handleNotesChange = useDebouncedCallback((event: ChangeEvent<HTMLTextAreaElement>) => {
        if (!currentComponent || !currentNode) {
            return;
        }

        let nodeData: NodeDataType = {
            ...componentData,
            description: event.target.value,
            name: currentComponent.workflowNodeName,
        };

        if (currentNode.conditionData) {
            nodeData = updateNodeData(event.target.value, 'description') ?? nodeData;
        }

        saveWorkflowDefinition({
            decorative: true,
            nodeData,
            onSuccess: () => {
                setCurrentComponent({
                    ...currentComponent,
                    description: event.target.value,
                });

                setCurrentNode({
                    ...currentNode,
                    description: event.target.value,
                });
            },
            queryClient,
            updateWorkflowMutation,
            workflow,
        });
    }, 200);

    return (
        <div className="flex h-full flex-col gap-4 overflow-auto p-4">
            <fieldset className="space-y-1">
                <Label>Title</Label>

                <Input
                    defaultValue={currentComponent?.label}
                    key={`${currentComponent?.componentName}_nodeTitle`}
                    name="nodeTitle"
                    onChange={handleLabelChange}
                />
            </fieldset>

            <fieldset className="space-y-1">
                <Label>Notes</Label>

                <Textarea
                    defaultValue={currentComponent?.description || ''}
                    key={`${currentComponent?.componentName}_nodeNotes`}
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
