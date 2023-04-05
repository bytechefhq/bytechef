import {ComponentDefinitionModel} from 'middleware/definition-registry';

const PropertiesTab = ({
    currentComponent,
}: {
    currentComponent: ComponentDefinitionModel;
}) => {
    return <div>Properties for {currentComponent.display.title}</div>;
};

export default PropertiesTab;
