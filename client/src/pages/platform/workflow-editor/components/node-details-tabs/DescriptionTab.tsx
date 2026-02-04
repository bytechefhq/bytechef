import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import {
    ClusterElementDefinition,
    ComponentDefinition,
    TaskDispatcherDefinition,
    TriggerDefinition,
} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {ChangeEvent} from 'react';
import {useDebouncedCallback} from 'use-debounce';
import {useShallow} from 'zustand/react/shallow';

import saveClusterElementFieldChange from '../../utils/saveClusterElementFieldChange';
import saveTaskDispatcherSubtaskFieldChange from '../../utils/saveTaskDispatcherSubtaskFieldChange';
import saveWorkflowDefinition from '../../utils/saveWorkflowDefinition';

interface DescriptionTabProps {
    invalidateWorkflowQueries: () => void;
    nodeDefinition: ComponentDefinition | ClusterElementDefinition | TaskDispatcherDefinition | TriggerDefinition;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

const DescriptionTab = ({invalidateWorkflowQueries, nodeDefinition, updateWorkflowMutation}: DescriptionTabProps) => {
    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentComponent: state.currentComponent,
            currentNode: state.currentNode,
            setCurrentComponent: state.setCurrentComponent,
            setCurrentNode: state.setCurrentNode,
        }))
    );
    const {nodes, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
            workflow: state.workflow,
        }))
    );

    const handleLabelChange = useDebouncedCallback((event: ChangeEvent<HTMLInputElement>) => {
        if (!currentNode) {
            return;
        }

        if (
            currentNode.conditionData ||
            currentNode.loopData ||
            currentNode.branchData ||
            currentNode.parallelData ||
            currentNode.eachData ||
            currentNode.forkJoinData
        ) {
            saveTaskDispatcherSubtaskFieldChange({
                currentComponentDefinition: nodeDefinition as ComponentDefinition,
                currentNodeIndex: nodes.findIndex((node) => node.data.name === currentNode.workflowNodeName),
                fieldUpdate: {
                    field: 'label',
                    value: event.target.value,
                },
                invalidateWorkflowQueries,
                updateWorkflowMutation,
            });

            return;
        }

        if (currentNode.clusterElementType || currentNode.clusterRoot) {
            saveClusterElementFieldChange({
                currentComponentDefinition: nodeDefinition as ComponentDefinition,
                fieldUpdate: {
                    field: 'label',
                    value: event.target.value,
                },
                invalidateWorkflowQueries,
                updateWorkflowMutation,
            });

            return;
        }

        saveWorkflowDefinition({
            decorative: true,
            invalidateWorkflowQueries,
            nodeData: {
                ...currentNode,
                label: event.target.value,
                name: currentNode.workflowNodeName,
                version: 'version' in nodeDefinition ? nodeDefinition.version : 1,
            },
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
            updateWorkflowMutation,
        });
    }, 600);

    const handleNotesChange = useDebouncedCallback((event: ChangeEvent<HTMLTextAreaElement>) => {
        if (!currentNode) {
            return;
        }

        if (
            currentNode.conditionData ||
            currentNode.loopData ||
            currentNode.branchData ||
            currentNode.parallelData ||
            currentNode.eachData ||
            currentNode.forkJoinData
        ) {
            saveTaskDispatcherSubtaskFieldChange({
                currentComponentDefinition: nodeDefinition as ComponentDefinition,
                currentNodeIndex: nodes.findIndex((node) => node.data.name === currentNode.workflowNodeName),
                fieldUpdate: {
                    field: 'description',
                    value: event.target.value,
                },
                invalidateWorkflowQueries,
                updateWorkflowMutation,
            });

            return;
        }

        if (currentNode.clusterElementType || currentNode.clusterRoot) {
            saveClusterElementFieldChange({
                currentComponentDefinition: nodeDefinition as ComponentDefinition,
                fieldUpdate: {
                    field: 'description',
                    value: event.target.value,
                },
                invalidateWorkflowQueries,
                updateWorkflowMutation,
            });

            return;
        }

        saveWorkflowDefinition({
            decorative: true,
            invalidateWorkflowQueries,
            nodeData: {
                ...currentNode,
                description: event.target.value,
                name: currentNode.workflowNodeName,
                version: 'version' in nodeDefinition ? nodeDefinition.version : 1,
            },
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
            updateWorkflowMutation,
        });
    }, 600);

    let workflowTaskOrTrigger =
        workflow.triggers?.find((trigger) => trigger.name === currentNode?.workflowNodeName) ||
        (currentNode?.workflowNodeName
            ? getTask({
                  tasks: workflow.tasks || [],
                  workflowNodeName: currentNode.workflowNodeName,
              })
            : undefined);

    if (!workflowTaskOrTrigger && currentNode?.clusterElementType) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        workflowTaskOrTrigger = currentNode as any;
    }

    return (
        <div className="flex h-full flex-col gap-4 overflow-auto p-4">
            <fieldset className="space-y-1">
                <Label>Title</Label>

                <Input
                    className="bg-white"
                    defaultValue={workflowTaskOrTrigger?.label}
                    key={`${currentNode?.componentName}-${workflowTaskOrTrigger?.type}_nodeTitle`}
                    name="nodeTitle"
                    onChange={handleLabelChange}
                />
            </fieldset>

            <fieldset className="space-y-1">
                <Label>Notes</Label>

                <Textarea
                    className="bg-white"
                    defaultValue={workflowTaskOrTrigger?.description}
                    key={`${currentNode?.componentName}-${workflowTaskOrTrigger?.type}_nodeNotes`}
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
