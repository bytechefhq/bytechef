import ComboBox, {ComboBoxItemType} from '@/components/ComboBox/ComboBox';
import {IntegrationModel, IntegrationStatusModel} from '@/shared/middleware/embedded/configuration';
import {ComponentDefinitionBasicModel} from '@/shared/middleware/platform/configuration';
import {useGetIntegrationsQuery} from '@/shared/queries/embedded/integrations.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {FocusEventHandler} from 'react';
import InlineSVG from 'react-inlinesvg';

const IntegrationLabel = ({
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

const IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox = ({
    onBlur,
    onChange,
    value,
}: {
    onBlur: FocusEventHandler;
    onChange: (item?: ComboBoxItemType) => void;
    value?: number;
}) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const {data: integrations} = useGetIntegrationsQuery({status: IntegrationStatusModel.Published});

    return integrations && componentDefinitions ? (
        <ComboBox
            items={integrations.map((integration) => {
                const componentDefinition = componentDefinitions.filter(
                    (componentDefinition) => componentDefinition.name === integration.componentName
                )[0];

                return {
                    label: <IntegrationLabel componentDefinition={componentDefinition} integration={integration} />,
                    name: componentDefinition.title,
                    value: integration.id,
                } as ComboBoxItemType;
            })}
            name="integrationId"
            onBlur={onBlur}
            onChange={onChange}
            value={value}
        />
    ) : (
        <>Loading...</>
    );
};

export default IntegrationInstanceConfigurationDialogBasicStepIntegrationsComboBox;
