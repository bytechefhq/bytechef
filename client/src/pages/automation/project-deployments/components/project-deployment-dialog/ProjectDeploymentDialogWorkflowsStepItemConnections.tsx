import DeploymentConnectionFormField from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItemConnection';
import {ComponentConnection, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {Control, UseFormSetValue} from 'react-hook-form';

type ComponentConnectionWithIndexType = {
    connection: ComponentConnection;
    originalIndex: number;
};
interface ProjectDeploymentDialogWorkflowsStepItemConnectionsProps {
    componentConnections: ComponentConnection[];
    control: Control<ProjectDeployment>;
    setValue: UseFormSetValue<ProjectDeployment>;
    workflowIndex: number;
}

const ProjectDeploymentDialogWorkflowsStepItemConnections = ({
    componentConnections,
    control,
    setValue,
    workflowIndex,
}: ProjectDeploymentDialogWorkflowsStepItemConnectionsProps) => {
    const componentConnectionsLength = componentConnections.length;

    if (!componentConnectionsLength) {
        return <p className="text-sm">No defined connections.</p>;
    }

    const connectionGroupMap = new Map<string, ComponentConnectionWithIndexType[]>();

    for (const [index, connection] of componentConnections.entries()) {
        const componentName = connection.componentName;

        if (!connectionGroupMap.has(componentName)) {
            connectionGroupMap.set(componentName, []);
        }

        connectionGroupMap.get(componentName)!.push({connection, originalIndex: index});
    }

    const connectionGroups = Array.from(connectionGroupMap.values());

    return (
        <ul className="space-y-4">
            {connectionGroups.map((group) => {
                const representative = group[0];
                const groupedIndices = group.map((entry) => entry.originalIndex);
                const connectionGrouping = {indices: groupedIndices, setValue};

                return (
                    <DeploymentConnectionFormField
                        componentConnection={representative.connection}
                        componentConnectionIndex={representative.originalIndex}
                        connectionGrouping={connectionGrouping}
                        control={control}
                        key={`grouped_${representative.connection.componentName}`}
                        workflowIndex={workflowIndex}
                    />
                );
            })}
        </ul>
    );
};

export default ProjectDeploymentDialogWorkflowsStepItemConnections;
