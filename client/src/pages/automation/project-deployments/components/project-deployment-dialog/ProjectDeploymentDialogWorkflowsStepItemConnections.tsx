import ProjectDeploymentDialogWorkflowsStepItemConnection from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItemConnection';
import {ComponentConnection, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {Control, UseFormSetValue} from 'react-hook-form';

interface ProjectDeploymentDialogWorkflowsStepItemConnectionsProps {
    componentConnections: ComponentConnection[];
    control: Control<ProjectDeployment>;
    groupConnections?: boolean;
    setValue: UseFormSetValue<ProjectDeployment>;
    workflowIndex: number;
    workflowNodeLabelMap: Map<string, string>;
}

type ComponentConnectionWithIndexType = {
    connection: ComponentConnection;
    originalIndex: number;
};

const ProjectDeploymentDialogWorkflowsStepItemConnections = ({
    componentConnections,
    control,
    groupConnections,
    setValue,
    workflowIndex,
    workflowNodeLabelMap,
}: ProjectDeploymentDialogWorkflowsStepItemConnectionsProps) => {
    const componentConnectionsLength = componentConnections.length;

    if (!componentConnectionsLength) {
        return <p className="text-sm">No defined connections.</p>;
    }

    if (!groupConnections) {
        return (
            <ul className="space-y-4">
                {componentConnections.map((componentConnection, componentConnectionIndex) => (
                    <ProjectDeploymentDialogWorkflowsStepItemConnection
                        componentConnection={componentConnection}
                        componentConnectionIndex={componentConnectionIndex}
                        control={control}
                        key={`${componentConnectionIndex}_${componentConnection.key}`}
                        workflowIndex={workflowIndex}
                        workflowNodeLabel={workflowNodeLabelMap.get(componentConnection.workflowNodeName)}
                    />
                ))}
            </ul>
        );
    }

    const groupMap = new Map<string, ComponentConnectionWithIndexType[]>();

    for (const [index, connection] of componentConnections.entries()) {
        const key = connection.componentName;

        if (!groupMap.has(key)) {
            groupMap.set(key, []);
        }

        groupMap.get(key)!.push({connection, originalIndex: index});
    }

    const connectionGroups = Array.from(groupMap.values());

    return (
        <ul className="space-y-4">
            {connectionGroups.map((group) => {
                const representative = group[0];
                const groupedIndices = group.map((entry) => entry.originalIndex);
                const connectionGrouping = {indices: groupedIndices, setValue};

                return (
                    <ProjectDeploymentDialogWorkflowsStepItemConnection
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
