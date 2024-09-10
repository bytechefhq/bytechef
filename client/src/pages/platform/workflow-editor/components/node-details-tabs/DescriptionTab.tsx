import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {NodeDataType, UpdateWorkflowMutationType} from '@/shared/types';
import {ChangeEvent} from 'react';
import {useDebouncedCallback} from 'use-debounce';

import saveWorkflowDefinition from '../../utils/saveWorkflowDefinition';

const DescriptionTab = ({updateWorkflowMutation}: {updateWorkflowMutation: UpdateWorkflowMutationType}) => {
    const {workflow} = useWorkflowDataStore();
    const {currentComponent, currentNode, setCurrentComponent} = useWorkflowNodeDetailsPanelStore();

    const componentData: NodeDataType = {
        componentName: currentComponent!.componentName!,
        description: currentComponent?.notes,
        icon: currentNode?.icon,
        label: currentComponent?.title,
        name: currentNode!.workflowNodeName!,
        operationName: currentComponent?.operationName,
        trigger: !!currentNode?.trigger,
        type: currentComponent?.type,
        workflowNodeName: currentNode?.workflowNodeName,
    };

    const handleLabelChange = useDebouncedCallback((event: ChangeEvent<HTMLInputElement>) => {
        if (!currentComponent || !currentNode) {
            return;
        }

        saveWorkflowDefinition(
            {...componentData, label: event.target.value},
            workflow,
            updateWorkflowMutation,
            undefined,
            () => {
                setCurrentComponent({
                    ...currentComponent,
                    title: event.target.value,
                });
            }
        );
    }, 200);

    const handleNotesChange = useDebouncedCallback((event: ChangeEvent<HTMLTextAreaElement>) => {
        if (!currentComponent || !currentNode) {
            return;
        }

        saveWorkflowDefinition(
            {
                ...componentData,
                description: event.target.value,
            },
            workflow,
            updateWorkflowMutation,
            undefined,
            () => {
                setCurrentComponent({
                    ...currentComponent,
                    notes: event.target.value,
                });
            }
        );
    }, 200);

    return (
        <div className="flex h-full flex-col gap-4 overflow-auto p-4">
            <fieldset className="space-y-2">
                <Label>Title</Label>

                <Input
                    defaultValue={currentComponent?.title}
                    key={`${currentComponent?.componentName}_nodeTitle`}
                    name="nodeTitle"
                    onChange={handleLabelChange}
                />
            </fieldset>

            <fieldset className="space-y-2">
                <Label>Notes</Label>

                <Textarea
                    className="mt-1"
                    defaultValue={currentComponent?.notes || ''}
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
