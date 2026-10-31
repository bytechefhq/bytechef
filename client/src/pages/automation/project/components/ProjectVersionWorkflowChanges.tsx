import VersionWorkflowChangesList from '@/shared/components/version-workflow-changes/VersionWorkflowChangesList';
import {useProjectVersionWorkflowChangesQuery} from '@/shared/middleware/graphql';
import {useState} from 'react';

import ProjectWorkflowVersionChangeDialog from './ProjectWorkflowVersionChangeDialog';

interface ProjectVersionWorkflowChangesProps {
    draft: boolean;
    expanded: boolean;
    projectId: number;
    projectVersion: number;
}

const ProjectVersionWorkflowChanges = ({
    draft,
    expanded,
    projectId,
    projectVersion,
}: ProjectVersionWorkflowChangesProps) => {
    const [selectedWorkflowUuid, setSelectedWorkflowUuid] = useState<string | undefined>();

    const {data, isLoading} = useProjectVersionWorkflowChangesQuery(
        {projectId: String(projectId), projectVersion},
        {enabled: expanded}
    );

    return (
        <>
            <VersionWorkflowChangesList
                changes={data?.projectVersionWorkflowChanges}
                isLoading={isLoading}
                onWorkflowClick={setSelectedWorkflowUuid}
            />

            {selectedWorkflowUuid && (
                <ProjectWorkflowVersionChangeDialog
                    draft={draft}
                    onClose={() => setSelectedWorkflowUuid(undefined)}
                    projectId={projectId}
                    projectVersion={projectVersion}
                    workflowUuid={selectedWorkflowUuid}
                />
            )}
        </>
    );
};

export default ProjectVersionWorkflowChanges;
