import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import {UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {ComponentType, CurrentComponentDefinitionType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';
import {ComponentDefinitionModel, WorkflowModel} from 'middleware/platform/configuration';
import {ChangeEvent} from 'react';

import {useWorkflowNodeDetailsPanelStore} from '../../stores/useWorkflowNodeDetailsPanelStore';
import saveWorkflowDefinition from '../../utils/saveWorkflowDefinition';

const DescriptionTab = ({
    componentDefinition,
    currentComponent,
    otherComponents,
    updateWorkflowMutation,
}: {
    componentDefinition: ComponentDefinitionModel;
    currentComponent: ComponentType | undefined;
    otherComponents: Array<ComponentType>;
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}) => {
    const {setComponents, workflow} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const {name, title, workflowNodeName} = componentDefinition as CurrentComponentDefinitionType;

    const currentWorkflowTask = workflow?.tasks?.find((task) => task.name === workflowNodeName);

    const handleLabelChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (!currentComponent) {
            return;
        }

        setComponents([
            ...otherComponents,
            {
                ...currentComponent,
                title: event.target.value,
            },
        ]);

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
            setComponents([
                ...otherComponents,
                {
                    ...currentComponent,
                    notes: event.target.value,
                },
            ]);
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
