import {ComponentDefinitionModel} from 'middleware/definition-registry';

const PropertiesTab = ({component}: {component: ComponentDefinitionModel}) => {
    return <div>Properties for {component.display.title}</div>;
};

export default PropertiesTab;
