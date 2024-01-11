import {ComponentDefinitionBasicModel} from '@/middleware/platform/configuration';
import UnifiedAPIIntegrationListItem from 'pages/embedded/integrations/components/UnifiedAPIIntegrationListItem';

const UnifiedAPIIntegrationList = ({componentDefinitions}: {componentDefinitions: ComponentDefinitionBasicModel[]}) => {
    return (
        <div className="w-full divide-y divide-gray-100 px-2 3xl:mx-auto 3xl:w-4/5">
            {componentDefinitions.map((componentDefinition) => {
                return (
                    <UnifiedAPIIntegrationListItem
                        componentDefinition={componentDefinition}
                        key={componentDefinition.name}
                    />
                );
            })}
        </div>
    );
};
export default UnifiedAPIIntegrationList;
