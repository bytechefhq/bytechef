import {ComponentDataType} from '@/types/types';
import Input from 'components/Input/Input';
import TextArea from 'components/TextArea/TextArea';
import {ComponentDefinitionModel} from 'middleware/hermes/configuration';
import {ChangeEvent} from 'react';

import useWorkflowDefinitionStore from '../../stores/useWorkflowDefinitionStore';

const DescriptionTab = ({
    componentDefinition,
    currentComponentData,
    otherComponentData,
}: {
    componentDefinition: ComponentDefinitionModel;
    currentComponentData: ComponentDataType | undefined;
    otherComponentData: Array<ComponentDataType>;
}) => {
    const {name} = componentDefinition;
    const {setComponentData} = useWorkflowDefinitionStore();

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
                defaultValue={
                    currentComponentData?.title || componentDefinition.title
                }
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
                onChange={handleNotesChange}
                placeholder="Write some notes for yourself..."
                value={currentComponentData?.notes || ''}
            />
        </div>
    );
};

export default DescriptionTab;
