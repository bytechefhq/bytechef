import Input from 'components/Input/Input';
import TextArea from 'components/TextArea/TextArea';
import {ComponentDefinitionModel} from 'middleware/core/definition-registry';

const DescriptionTab = ({component}: {component: ComponentDefinitionModel}) => (
    <div className="h-full flex-[1_1_1px] overflow-auto p-4">
        <Input
            label="Name"
            labelClassName="px-2"
            name="componentDescriptionName"
            defaultValue={component.title}
        />

        <TextArea
            label="Description"
            labelClassName="px-2"
            name="nodeDescription"
            placeholder="Write some notes for yourself..."
        />
    </div>
);

export default DescriptionTab;
