import IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection';
import {IntegrationInstanceConfigurationModel} from '@/shared/middleware/embedded/configuration';
import {WorkflowConnectionModel} from '@/shared/middleware/platform/configuration';
import {Control} from 'react-hook-form';

export interface IntegrationInstanceConfigurationDialogWorkflowsStepItemConnectionProps {
    control: Control<IntegrationInstanceConfigurationModel>;
    workflowConnection: WorkflowConnectionModel;
    workflowConnectionIndex: number;
    workflowIndex: number;
}

const IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections = ({
    control,
    workflowConnections,
    workflowIndex,
}: {
    control: Control<IntegrationInstanceConfigurationModel>;
    workflowConnections: WorkflowConnectionModel[];
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
