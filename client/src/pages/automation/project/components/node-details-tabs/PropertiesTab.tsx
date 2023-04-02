import {ComponentDefinitionWithBasicActionsModel} from 'middleware/definition-registry';

const PropertiesTab = ({
    currentComponent,
}: {
    currentComponent: ComponentDefinitionWithBasicActionsModel;
}) => {
    return <div>Properties for {currentComponent.display.label}</div>;
};

export default PropertiesTab;
