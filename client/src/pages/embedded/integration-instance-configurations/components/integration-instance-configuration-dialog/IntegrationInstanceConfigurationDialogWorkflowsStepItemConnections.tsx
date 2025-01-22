import IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection from '@/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection';
import {IntegrationInstanceConfiguration} from '@/shared/middleware/embedded/configuration';
import {ComponentConnection} from '@/shared/middleware/platform/configuration';
import {Control} from 'react-hook-form';

const IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections = ({
    componentConnections,
    control,
    workflowIndex,
}: {
    control: Control<IntegrationInstanceConfiguration>;
    componentConnections: ComponentConnection[];
    workflowIndex: number;
}) => {
    return componentConnections.length ? (
        <>
            {componentConnections.map((componentConnection, componentConnectionIndex) => (
                <IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection
                    componentConnection={componentConnection}
                    componentConnectionIndex={componentConnectionIndex}
                    control={control}
                    key={componentConnectionIndex + '_' + componentConnection.key}
                    workflowIndex={workflowIndex}
                />
            ))}
        </>
    ) : (
        <p className="text-sm">No defined connections.</p>
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections;
