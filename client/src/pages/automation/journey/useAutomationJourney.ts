import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useGetWorkspaceProjectDeploymentsQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

import {type AutomationJourneyI, getAutomationJourney} from './getAutomationJourney';

interface UseAutomationJourneyI {
    isLoading: boolean;
    journey?: AutomationJourneyI;
}

/*
 * Call it only from an empty state that mounts when the page's own list is empty, so a page with items makes no
 * extra requests. Both query keys match the Projects and Deployments pages' unfiltered lists, so they are often cached.
 */
export const useAutomationJourney = (): UseAutomationJourneyI => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    // Unfiltered and with all fields: projectWorkflowIds is only returned when includeAllFields is true.
    const {data: projects, isLoading: projectsIsLoading} = useGetWorkspaceProjectsQuery({id: currentWorkspaceId});

    const {data: projectDeployments, isLoading: projectDeploymentsIsLoading} = useGetWorkspaceProjectDeploymentsQuery({
        environmentId: currentEnvironmentId,
        id: currentWorkspaceId,
    });

    const journey = useMemo(
        () => (projects && projectDeployments ? getAutomationJourney(projects, projectDeployments) : undefined),
        [projectDeployments, projects]
    );

    return {
        isLoading: projectsIsLoading || projectDeploymentsIsLoading,
        journey,
    };
};
