import VersionWorkflowChangesList from '@/shared/components/version-workflow-changes/VersionWorkflowChangesList';
import {useIntegrationVersionWorkflowChangesQuery} from '@/shared/middleware/graphql';
import {useState} from 'react';

import IntegrationWorkflowVersionChangeDialog from './IntegrationWorkflowVersionChangeDialog';

interface IntegrationVersionWorkflowChangesProps {
    draft: boolean;
    expanded: boolean;
    integrationId: number;
    integrationVersion: number;
}

const IntegrationVersionWorkflowChanges = ({
    draft,
    expanded,
    integrationId,
    integrationVersion,
}: IntegrationVersionWorkflowChangesProps) => {
    const [selectedWorkflowUuid, setSelectedWorkflowUuid] = useState<string | undefined>();

    const {data, isLoading} = useIntegrationVersionWorkflowChangesQuery(
        {integrationId: String(integrationId), integrationVersion},
        {enabled: expanded}
    );

    return (
        <>
            <VersionWorkflowChangesList
                changes={data?.integrationVersionWorkflowChanges}
                isLoading={isLoading}
                onWorkflowClick={setSelectedWorkflowUuid}
            />

            {selectedWorkflowUuid && (
                <IntegrationWorkflowVersionChangeDialog
                    draft={draft}
                    integrationId={integrationId}
                    integrationVersion={integrationVersion}
                    onClose={() => setSelectedWorkflowUuid(undefined)}
                    workflowUuid={selectedWorkflowUuid}
                />
            )}
        </>
    );
};

export default IntegrationVersionWorkflowChanges;
