import {ComponentDefinitionModel} from 'middleware/definition-registry';

const OutputTab = ({
    currentComponent,
}: {
    currentComponent: ComponentDefinitionModel;
}) => {
    return <div>Output for {currentComponent.display.title}</div>;
};

export default OutputTab;
