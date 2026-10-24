import EmptyList from '@/components/EmptyList';
import useDataSyncs from '@/pages/automation/data-syncs/hooks/useDataSyncs';
import {ProjectDeploymentWorkflow} from '@/shared/middleware/automation/configuration';
import {ArrowLeftRightIcon} from 'lucide-react';
import {useMemo} from 'react';

import ProjectDeploymentDataSyncListItem, {ProjectDeploymentDataSyncType} from './ProjectDeploymentDataSyncListItem';

interface ProjectDeploymentDataSyncListProps {
    dataSyncDeployments: ProjectDeploymentDataSyncType[];
    projectDeploymentWorkflows: ProjectDeploymentWorkflow[];
}

const ProjectDeploymentDataSyncList = ({
    dataSyncDeployments,
    projectDeploymentWorkflows,
}: ProjectDeploymentDataSyncListProps) => {
    const {dataSyncs} = useDataSyncs();

    const projectWorkflowUuidByDataSyncId = useMemo(
        () => new Map(dataSyncs.map((dataSync) => [dataSync.id, dataSync.projectWorkflowUuid])),
        [dataSyncs]
    );

    if (dataSyncDeployments.length === 0) {
        return (
            <div className="flex justify-center py-8">
                <EmptyList
                    icon={<ArrowLeftRightIcon className="size-24 text-stroke-neutral-tertiary" />}
                    message="This deployment has no data syncs."
                    title="No Data Syncs"
                />
            </div>
        );
    }

    return (
        <ul className="divide-y divide-stroke-neutral-primary pt-3">
            {dataSyncDeployments.map((dataSyncDeployment) => {
                const projectWorkflowUuid = projectWorkflowUuidByDataSyncId.get(dataSyncDeployment.dataSyncId);

                return (
                    <ProjectDeploymentDataSyncListItem
                        dataSyncDeployment={dataSyncDeployment}
                        key={dataSyncDeployment.dataSyncId}
                        projectDeploymentWorkflow={projectDeploymentWorkflows.find(
                            (projectDeploymentWorkflow) =>
                                projectDeploymentWorkflow.workflowUuid != null &&
                                projectDeploymentWorkflow.workflowUuid === projectWorkflowUuid
                        )}
                    />
                );
            })}
        </ul>
    );
};

export default ProjectDeploymentDataSyncList;
