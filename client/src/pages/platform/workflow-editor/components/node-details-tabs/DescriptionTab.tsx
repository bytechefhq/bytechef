import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {UpdateWorkflowRequestI} from '@/mutations/platform/workflows.mutations';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {UseMutationResult} from '@tanstack/react-query';
import {WorkflowModel} from 'middleware/platform/configuration';
import {ChangeEvent} from 'react';

import saveWorkflowDefinition from '../../utils/saveWorkflowDefinition';

const DescriptionTab = ({
    updateWorkflowMutation,
}: {
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequestI, unknown>;
}) => {
    const {workflow} = useWorkflowDataStore();
    const {currentComponent} = useWorkflowNodeDetailsPanelStore();

    const handleLabelChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (!currentComponent) {
            return;
        }

        if (currentComponent?.componentName) {
            saveWorkflowDefinition(
                {
                    componentName: currentComponent.componentName as string,
                    icon: undefined,
                    label: event.target.value,
                    name: currentComponent.workflowNodeName,
                },
                workflow,
                updateWorkflowMutation
            );
        }
    };

    const handleNotesChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
        if (currentComponent?.componentName) {
            saveWorkflowDefinition(
                {
                    componentName: currentComponent.componentName as string,
                    description: event.target.value,
                    icon: undefined,
                    name: currentComponent.workflowNodeName,
                },
                workflow,
                updateWorkflowMutation
            );
        }
    };

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
                />
            </fieldset>
        </div>
    );
};

export default DescriptionTab;
