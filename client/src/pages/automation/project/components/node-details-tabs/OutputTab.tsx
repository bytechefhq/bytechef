import {ComponentDefinitionWithBasicActionsModel} from 'middleware/definition-registry';

const OutputTab = ({
    currentComponent,
}: {
    currentComponent: ComponentDefinitionWithBasicActionsModel;
}) => {
    return <div>Output for {currentComponent.display.label}</div>;
};

export default OutputTab;
