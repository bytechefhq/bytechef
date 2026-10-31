import VersionWorkflowChangesList from '@/shared/components/version-workflow-changes/VersionWorkflowChangesList';
import {useAutomationWorkflowProjectVersionWorkflowChangesQuery} from '@/shared/middleware/graphql';
import {useState} from 'react';

import AutomationWorkflowProjectWorkflowVersionChangeDialog from './AutomationWorkflowProjectWorkflowVersionChangeDialog';

interface AutomationWorkflowProjectVersionWorkflowChangesProps {
    draft: boolean;
    expanded: boolean;
    projectId: string;
    projectVersion: number;
}

const AutomationWorkflowProjectVersionWorkflowChanges = ({
    draft,
    expanded,
    projectId,
    projectVersion,
}: AutomationWorkflowProjectVersionWorkflowChangesProps) => {
    const [selectedWorkflowUuid, setSelectedWorkflowUuid] = useState<string | undefined>();

    const {data, isLoading} = useAutomationWorkflowProjectVersionWorkflowChangesQuery(
        {id: projectId, projectVersion},
        {enabled: expanded}
    );

    return (
        <>
            <VersionWorkflowChangesList
                changes={data?.automationWorkflowProjectVersionWorkflowChanges}
                isLoading={isLoading}
                onWorkflowClick={setSelectedWorkflowUuid}
            />

            {selectedWorkflowUuid && (
                <AutomationWorkflowProjectWorkflowVersionChangeDialog
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

export default AutomationWorkflowProjectVersionWorkflowChanges;
