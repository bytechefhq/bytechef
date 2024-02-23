import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {ComponentDataType} from '@/types/types';
import {ComponentDefinitionModel} from 'middleware/platform/configuration';
import {ChangeEvent} from 'react';

const DescriptionTab = ({
    componentDefinition,
    currentComponentData,
    otherComponentData,
}: {
    componentDefinition: ComponentDefinitionModel;
    currentComponentData: ComponentDataType | undefined;
    otherComponentData: Array<ComponentDataType>;
}) => {
    const {name, title} = componentDefinition;
    const {setComponentData} = useWorkflowDataStore();

    const handleTitleChange = (event: ChangeEvent<HTMLInputElement>) => {
        if (currentComponentData) {
            setComponentData([
                ...otherComponentData,
                {
                    ...currentComponentData,
                    title: event.target.value,
                },
            ]);
        }
    };

    const handleNotesChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
        if (currentComponentData) {
            setComponentData([
                ...otherComponentData,
                {
                    ...currentComponentData,
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
                    defaultValue={currentComponentData?.title || title}
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
                    value={currentComponentData?.notes || ''}
                />
            </div>
        </div>
    );
};

export default DescriptionTab;
