import IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection from '@/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection';
import {IntegrationInstanceConfiguration} from '@/shared/middleware/embedded/configuration';
import {WorkflowConnection} from '@/shared/middleware/platform/configuration';
import {Control} from 'react-hook-form';

const IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections = ({
    control,
    workflowConnections,
    workflowIndex,
}: {
    control: Control<IntegrationInstanceConfiguration>;
    workflowConnections: WorkflowConnection[];
    workflowIndex: number;
}) => {
    return workflowConnections.length ? (
        <>
            {workflowConnections.map((workflowConnection, workflowConnectionIndex) => (
                <IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection
                    control={control}
                    key={workflowConnectionIndex + '_' + workflowConnection.key}
                    workflowConnection={workflowConnection}
                    workflowConnectionIndex={workflowConnectionIndex}
                    workflowIndex={workflowIndex}
                />
            ))}
        </>
    ) : (
        <p className="text-sm">No defined connections.</p>
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections;
