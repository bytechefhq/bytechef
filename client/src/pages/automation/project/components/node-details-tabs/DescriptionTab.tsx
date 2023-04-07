import Input from 'components/Input/Input';
import TextArea from 'components/TextArea/TextArea';
import {ComponentDefinitionModel} from 'middleware/definition-registry';

const DescriptionTab = ({component}: {component: ComponentDefinitionModel}) => (
    <>
        <Input
            label="Name"
            labelClassName="px-2"
            name="componentDescriptionName"
            defaultValue={component.display.title}
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
