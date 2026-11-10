import VersionWorkflowChangesList from '@/shared/components/version-workflow-changes/VersionWorkflowChangesList';
import {useAutomationProjectVersionWorkflowChangesQuery} from '@/shared/middleware/graphql';
import {useState} from 'react';

import AutomationProjectWorkflowVersionChangeDialog from './AutomationProjectWorkflowVersionChangeDialog';

interface AutomationProjectVersionWorkflowChangesProps {
    draft: boolean;
    expanded: boolean;
    projectId: string;
    projectVersion: number;
}

const AutomationProjectVersionWorkflowChanges = ({
    draft,
    expanded,
    projectId,
    projectVersion,
}: AutomationProjectVersionWorkflowChangesProps) => {
    const [selectedWorkflowUuid, setSelectedWorkflowUuid] = useState<string | undefined>();

    const {data, isError, isLoading} = useAutomationProjectVersionWorkflowChangesQuery(
        {id: projectId, projectVersion},
        {enabled: expanded}
    );

    return (
        <>
            <VersionWorkflowChangesList
                changes={data?.automationProjectVersionWorkflowChanges}
                isError={isError}
                isLoading={isLoading}
                onWorkflowClick={setSelectedWorkflowUuid}
            />

            {selectedWorkflowUuid && (
                <AutomationProjectWorkflowVersionChangeDialog
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

export default AutomationProjectVersionWorkflowChanges;
