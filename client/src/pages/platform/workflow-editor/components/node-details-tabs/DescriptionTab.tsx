import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {
    ComponentDefinition,
    TaskDispatcherDefinition,
    TriggerDefinition,
} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {ChangeEvent} from 'react';
import {useParams} from 'react-router-dom';
import {useDebouncedCallback} from 'use-debounce';
import {useShallow} from 'zustand/react/shallow';

import saveTaskDispatcherSubtaskFieldChange from '../../utils/saveTaskDispatcherSubtaskFieldChange';
import saveWorkflowDefinition from '../../utils/saveWorkflowDefinition';

const DescriptionTab = ({
    nodeDefinition,
    updateWorkflowMutation,
}: {
    nodeDefinition: ComponentDefinition | TaskDispatcherDefinition | TriggerDefinition;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}) => {
    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore();
    const {workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
        }))
    );

    const workflowTaskOrTrigger = [...(workflow.tasks ?? []), ...(workflow.triggers ?? [])]?.find(
        (task) => task.name === currentNode?.workflowNodeName
    );

    console.log('workflowTaskOrTrigger', workflowTaskOrTrigger);

    const queryClient = useQueryClient();
    const {projectId} = useParams();

    const handleLabelChange = useDebouncedCallback((event: ChangeEvent<HTMLInputElement>) => {
        if (!currentNode) {
            return;
        }

        if (currentNode.conditionData || currentNode.loopData || currentNode.branchData) {
            saveTaskDispatcherSubtaskFieldChange({
                currentComponentDefinition: nodeDefinition as ComponentDefinition,
                currentNodeIndex: nodes.findIndex((node) => node.data.name === currentNode.workflowNodeName),
                fieldUpdate: {
                    field: 'label',
                    value: event.target.value,
                },
                projectId: +projectId!,
                queryClient,
                updateWorkflowMutation,
            });

            return;
        }

        saveWorkflowDefinition({
            decorative: true,
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
            projectId: +projectId!,
            queryClient,
            updateWorkflowMutation,
        });
    }, 300);

    const handleNotesChange = useDebouncedCallback((event: ChangeEvent<HTMLTextAreaElement>) => {
        if (!currentNode) {
            return;
        }

        if (currentNode.conditionData || currentNode.loopData || currentNode.branchData) {
            saveTaskDispatcherSubtaskFieldChange({
                currentComponentDefinition: nodeDefinition as ComponentDefinition,
                currentNodeIndex: nodes.findIndex((node) => node.data.name === currentNode.workflowNodeName),
                fieldUpdate: {
                    field: 'description',
                    value: event.target.value,
                },
                projectId: +projectId!,
                queryClient,
                updateWorkflowMutation,
            });

            return;
        }

        saveWorkflowDefinition({
            decorative: true,
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
            projectId: +projectId!,
            queryClient,
            updateWorkflowMutation,
        });
    }, 300);

    return (
        <div className="flex h-full flex-col gap-4 overflow-auto p-4">
            <fieldset className="space-y-1">
                <Label>Title</Label>

                <Input
                    defaultValue={workflowTaskOrTrigger?.label}
                    key={`${currentNode?.componentName}-${workflowTaskOrTrigger?.type}_nodeTitle`}
                    name="nodeTitle"
                    onChange={handleLabelChange}
                />
            </fieldset>

            <fieldset className="space-y-1">
                <Label>Notes</Label>

                <Textarea
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
