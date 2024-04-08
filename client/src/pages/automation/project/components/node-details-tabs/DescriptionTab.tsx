import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {ComponentType} from '@/types/types';
import {ComponentDefinitionModel} from 'middleware/platform/configuration';
import {ChangeEvent} from 'react';

const DescriptionTab = ({
    componentDefinition,
    currentComponent,
    otherComponents,
}: {
    componentDefinition: ComponentDefinitionModel;
    currentComponent: ComponentType | undefined;
    otherComponents: Array<ComponentType>;
}) => {
    const {name, title} = componentDefinition;
    const {setComponents} = useWorkflowDataStore();

    const handleTitleChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (currentComponent) {
            setComponents([
                ...otherComponents,
                {
                    ...currentComponent,
                    title: event.target.value,
                },
            ]);
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
            <div className="space-y-2">
                <Label>Title</Label>

                <Input
                    defaultValue={currentComponent?.title || title}
                    key={`${name}_nodeTitle`}
                    name="nodeTitle"
                    onChange={handleTitleChange}
                />
            </div>

            <div className="space-y-2">
                <Label>Notes</Label>

                <Textarea
                    className="mt-1"
                    key={`${name}_nodeNotes`}
                    name="nodeNotes"
                    onChange={handleNotesChange}
                    placeholder="Write some notes for yourself..."
                    value={currentComponent?.notes || ''}
                />
            </div>
        </div>
    );
};

export default DescriptionTab;
