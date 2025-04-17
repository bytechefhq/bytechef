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

import getUpdatedRootBranchNodeData from '../../utils/getUpdatedRootBranchNodeData';
import getUpdatedRootConditionNodeData from '../../utils/getUpdatedRootConditionNodeData';
import getUpdatedRootLoopNodeData from '../../utils/getUpdatedRootLoopNodeData';
import saveWorkflowDefinition from '../../utils/saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from '../../utils/taskDispatcherConfig';

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

        if (currentNode.conditionData) {
            const parentConditionNode = nodes.find(
                (node) => node.data.name === currentNode?.conditionData?.conditionId
            );

            if (!parentConditionNode) {
                return;
            }

            const conditionCase = currentNode.conditionData.conditionCase;
            const conditionCaseSubtasks: Array<WorkflowTask> = (parentConditionNode.data as NodeDataType)?.parameters?.[
                conditionCase
            ];

            if (!conditionCaseSubtasks) {
                return;
            }

            const taskIndex = conditionCaseSubtasks.findIndex((subtask) => subtask.name === currentNode.name);

            if (taskIndex === -1) {
                return;
            }

            conditionCaseSubtasks[taskIndex] = {
                ...conditionCaseSubtasks[taskIndex],
                [field]: value,
            };

            nodeData = getUpdatedRootConditionNodeData({
                updatedParentNodeData: parentConditionNode.data as NodeDataType,
            });
        }

        if (currentNode.loopData) {
            const parentLoopNode = nodes.find((node) => node.data.name === currentNode?.loopData?.loopId);

            if (!parentLoopNode) {
                return;
            }

            const loopSubtasks: Array<WorkflowTask> = (parentLoopNode.data as NodeDataType)?.parameters?.iteratee;

            const taskIndex = loopSubtasks.findIndex((subtask) => subtask.name === currentNode.name);

            if (taskIndex === -1) {
                return;
            }

            loopSubtasks[taskIndex] = {
                ...loopSubtasks[taskIndex],
                [field]: value,
            };

            nodeData = getUpdatedRootLoopNodeData({
                updatedParentNodeData: parentLoopNode.data as NodeDataType,
            });
        }

        if (currentNode.branchData) {
            const parentBranchNode = nodes.find((node) => node.data.name === currentNode?.branchData?.branchId);

            if (!parentBranchNode) {
                return;
            }

            const branchCaseSubtasks = TASK_DISPATCHER_CONFIG.branch.getSubtasks({
                context: {
                    caseKey: currentNode.branchData.caseKey,
                    taskDispatcherId: currentNode.branchData.branchId,
                },
                node: parentBranchNode,
            });

            if (!branchCaseSubtasks) {
                return;
            }

            const taskIndex = branchCaseSubtasks.findIndex((subtask) => subtask.name === currentNode.name);

            if (taskIndex === -1) {
                return;
            }

            branchCaseSubtasks[taskIndex] = {
                ...branchCaseSubtasks[taskIndex],
                [field]: value,
            };

            nodeData = getUpdatedRootBranchNodeData({
                updatedParentNodeData: parentBranchNode.data as NodeDataType,
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

        if (currentNode.conditionData || currentNode.loopData || currentNode.branchData) {
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
