import Input from 'components/Input/Input';
import TextArea from 'components/TextArea/TextArea';
import {ComponentDefinitionWithBasicActionsModel} from 'middleware/definition-registry';

const DescriptionTab = ({
    currentComponent,
}: {
    currentComponent: ComponentDefinitionWithBasicActionsModel;
}) => (
    <>
        <Input
            label="Name"
            labelClassName="px-2"
            name="componentDescriptionName"
            defaultValue={currentComponent.display.label}
        />

        <TextArea
            label="Description"
            labelClassName="px-2"
            name="nodeDescription"
            placeholder="Write some notes for yourself..."
        />
    </>
);

export default DescriptionTab;
