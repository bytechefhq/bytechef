import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import InlineSVG from 'react-inlinesvg';

const EmbeddedWorkflowExecutionIntegration = ({componentName}: {componentName?: string}) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    if (!componentName) {
        return null;
    }

    const componentDefinition = componentDefinitions?.find(
        (currentComponentDefinition) => currentComponentDefinition.name === componentName
    );

    return (
        <div className="flex items-center gap-x-1.5">
            {componentDefinition?.icon && <InlineSVG className="size-4 flex-none" src={componentDefinition.icon} />}

            <span>{componentDefinition?.title || componentName}</span>
        </div>
    );
};

export default EmbeddedWorkflowExecutionIntegration;
