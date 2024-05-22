import ComboBox, {ComboBoxItemType} from '@/components/ComboBox';
import IntegrationInstanceConfigurationDialogBasicStepIntegrationLabel from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogBasicStepIntegrationLabel';
import {useGetIntegrationsQuery} from '@/shared/queries/embedded/integrations.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {FocusEventHandler} from 'react';

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

    const {data: integrations} = useGetIntegrationsQuery();

    return integrations && componentDefinitions ? (
        <ComboBox
            items={integrations.map((integration) => {
                const componentDefinition = componentDefinitions.filter(
                    (componentDefinition) => componentDefinition.name === integration.componentName
                )[0];

                return {
                    label: (
                        <IntegrationInstanceConfigurationDialogBasicStepIntegrationLabel
                            componentDefinition={componentDefinition}
                            integration={integration}
                        />
                    ),
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
