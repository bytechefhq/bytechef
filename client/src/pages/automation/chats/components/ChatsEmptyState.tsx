import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps from '@/components/JourneySteps/JourneySteps';
import PageLoader from '@/components/PageLoader';
import ProjectsStepButton from '@/pages/automation/journey/ProjectsStepButton';
import {CHATS_DOCUMENTATION_URL, getAutomationJourneySteps} from '@/pages/automation/journey/automationJourneySteps';
import {useAutomationJourney} from '@/pages/automation/journey/useAutomationJourney';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import {MessagesSquareIcon} from 'lucide-react';

/*
 * A chat needs a deployed, enabled workflow, so the page shows the same steps as Deployments, read from the user's own
 * projects and deployments, under a button that opens the project to work on.
 */
const ChatsEmptyState = () => {
    const environmentScopeNote = useEnvironmentScopeNote('Chats');
    const {isLoading, journey} = useAutomationJourney();

    if (isLoading) {
        return <PageLoader loading />;
    }

    return (
        <EmptyState
            actions={<ProjectsStepButton nextProject={journey?.nextProject} />}
            description="Chats let you talk to a deployed workflow, sending messages and files that each start a run. A workflow appears here once it has a Chat trigger in Hosted Chat mode and its deployment and workflow are both enabled."
            docsLink={{href: CHATS_DOCUMENTATION_URL, label: 'How chats work'}}
            icon={<MessagesSquareIcon />}
            note={environmentScopeNote}
            title="No chat workflows yet"
        >
            {journey && (
                <JourneySteps aria-label="Getting started" steps={getAutomationJourneySteps(journey.stepStatuses)} />
            )}
        </EmptyState>
    );
};

export default ChatsEmptyState;
