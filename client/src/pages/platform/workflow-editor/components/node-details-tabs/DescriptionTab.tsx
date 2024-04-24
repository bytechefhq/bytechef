import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {UpdateWorkflowRequest} from '@/middleware/platform/configuration';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {UseMutationResult} from '@tanstack/react-query';
import {ComponentDefinitionModel, WorkflowModel} from 'middleware/platform/configuration';
import {ChangeEvent} from 'react';

import saveWorkflowDefinition from '../../utils/saveWorkflowDefinition';

const DescriptionTab = ({
    componentDefinition,
    updateWorkflowMutation,
}: {
    componentDefinition: ComponentDefinitionModel;
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}) => {
    const {setComponent, workflow} = useWorkflowDataStore();
    const {currentComponent, currentNode} = useWorkflowNodeDetailsPanelStore();

    const {name, title} = componentDefinition;

    const currentWorkflowTask = workflow?.tasks?.find((task) => task.name === currentNode.name);

    const handleLabelChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (!currentComponent) {
            return;
        }

        setComponent({
            ...currentComponent,
            title: event.target.value,
        });

        if (currentNode.componentName) {
            saveWorkflowDefinition(
                {
                    ...currentNode,
                    componentName: currentComponent.componentName as string,
                    icon: currentComponent.icon,
                    label: event.target.value,
                },
                workflow,
                updateWorkflowMutation
            );
        }
    };

    const handleNotesChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
        if (currentComponent) {
            setComponent({
                ...currentComponent,
                notes: event.target.value,
            });
        }
    };

    return (
        <div className="flex h-full flex-col gap-4 overflow-auto p-4">
            <fieldset className="space-y-2">
                <Label>Title</Label>

                <Input
                    defaultValue={currentWorkflowTask?.label || title}
                    key={`${name}_nodeTitle`}
                    name="nodeTitle"
                    onChange={handleLabelChange}
                />
            </fieldset>

            <fieldset className="space-y-2">
                <Label>Notes</Label>

                <Textarea
                    className="mt-1"
                    key={`${name}_nodeNotes`}
                    name="nodeNotes"
                    onChange={handleNotesChange}
                    placeholder="Write some notes for yourself..."
                    value={currentComponent?.notes || ''}
                />
            </fieldset>
        </div>
    );
};

export default DescriptionTab;
