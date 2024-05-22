import {IntegrationModel} from '@/shared/middleware/embedded/configuration';
import {ComponentDefinitionBasicModel} from '@/shared/middleware/platform/configuration';
import InlineSVG from 'react-inlinesvg';

const IntegrationInstanceConfigurationDialogBasicStepIntegrationLabel = ({
    componentDefinition,
    integration,
}: {
    componentDefinition: ComponentDefinitionBasicModel;
    integration: IntegrationModel;
}) => (
    <div className="flex items-center gap-2">
        {componentDefinition?.icon && <InlineSVG className="size-6 flex-none" src={componentDefinition.icon} />}

        <span className="mr-1 ">{componentDefinition.title}</span>

        <span className="text-xs text-gray-500">{integration?.tags?.map((tag) => tag.name).join(', ')}</span>
    </div>
);

export default IntegrationInstanceConfigurationDialogBasicStepIntegrationLabel;
