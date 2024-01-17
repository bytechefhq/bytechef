import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {ComponentDataType} from '@/types/types';
import Input from 'components/Input/Input';
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

    const handleTitleChange = (event: ChangeEvent<HTMLInputElement>) =>
        setComponentData([
            ...otherComponentData,
            {
                ...currentComponentData,
                action: currentComponentData!.action,
                name,
                title: event.target.value,
            },
        ]);

    const handleNotesChange = (event: ChangeEvent<HTMLTextAreaElement>) =>
        setComponentData([
            ...otherComponentData,
            {
                ...currentComponentData,
                action: currentComponentData!.action,
                name,
                notes: event.target.value,
            },
        ]);

    return (
        <div className="h-full flex-[1_1_1px] overflow-auto p-4">
            <Input
                defaultValue={currentComponentData?.title || title}
                key={`${name}_nodeTitle`}
                label="Title"
                labelClassName="px-2"
                name="nodeTitle"
                onChange={handleTitleChange}
            />

            <div>
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
