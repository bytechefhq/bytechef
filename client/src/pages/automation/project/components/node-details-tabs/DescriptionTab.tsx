import {ComponentDataType} from '@/types/types';
import Input from 'components/Input/Input';
import TextArea from 'components/TextArea/TextArea';
import {ComponentDefinitionModel} from 'middleware/hermes/configuration';
import {ChangeEvent} from 'react';

import useWorkflowDefinitionStore from '../../stores/useWorkflowDefinitionStore';

const DescriptionTab = ({
    component,
    currentComponentData,
    otherComponentData,
}: {
    component: ComponentDefinitionModel;
    currentComponentData: ComponentDataType | undefined;
    otherComponentData: Array<ComponentDataType>;
}) => {
    const {name} = component;
    const {setComponentData} = useWorkflowDefinitionStore();

    const handleTitleChange = (event: ChangeEvent<HTMLInputElement>) =>
        setComponentData([
            ...otherComponentData,
            {
                ...currentComponentData,
                name: name,
                title: event.target.value,
            },
        ]);

    const handleNotesChange = (event: ChangeEvent<HTMLTextAreaElement>) =>
        setComponentData([
            ...otherComponentData,
            {
                ...currentComponentData,
                name: name,
                notes: event.target.value,
            },
        ]);

    return (
        <div className="h-full flex-[1_1_1px] overflow-auto p-4">
            <Input
                defaultValue={currentComponentData?.title || component.title}
                key={`${name}_nodeTitle`}
                label="Title"
                labelClassName="px-2"
                name="nodeTitle"
                onChange={handleTitleChange}
            />

            <TextArea
                key={`${name}_nodeNotes`}
                label="Notes"
                labelClassName="px-2"
                name="nodeNotes"
                placeholder="Write some notes for yourself..."
                onChange={handleNotesChange}
                value={currentComponentData?.notes || ''}
            />
        </div>
    );
};

export default DescriptionTab;
