import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps from '@/components/JourneySteps/JourneySteps';
import PageLoader from '@/components/PageLoader';
import ProjectsStepButton from '@/pages/automation/journey/ProjectsStepButton';
import {
    DEPLOYMENTS_DOCUMENTATION_URL,
    getAutomationJourneySteps,
} from '@/pages/automation/journey/automationJourneySteps';
import {type AutomationJourneyStageType} from '@/pages/automation/journey/getAutomationJourney';
import {useAutomationJourney} from '@/pages/automation/journey/useAutomationJourney';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import {Project, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {Layers3Icon, SendIcon} from 'lucide-react';

interface ProjectDeploymentsEmptyStateProps {
    onDeploymentCreated: (deploymentId: number) => void;
}

const getPublishDescription = (nextProject?: Project) =>
    `Deployments run a published, read-only version of a project. Open ${nextProject ? nextProject.name : 'a project'} and click Publish to create its first version.`;

/*
 * Points an empty Deployments page at the next step the user can finish. The deployment dialog only lists published
 * versions, so it is offered only once one exists; earlier stages send the user to their projects instead.
 */
const ProjectDeploymentsEmptyState = ({onDeploymentCreated}: ProjectDeploymentsEmptyStateProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {captureEmptyStateAction} = useAnalytics();
    const environmentScopeNote = useEnvironmentScopeNote('Deployments');
    const {isLoading, journey} = useAutomationJourney();

    if (isLoading) {
        return <PageLoader loading />;
    }

    // Without a journey (its queries failed) fall back to the deployment dialog, as before this page knew the stage.
    const stage: AutomationJourneyStageType = journey?.stage ?? 'deploy';

    const captureAction = (action: string) => captureEmptyStateAction('deployments', action, stage);

    const sharedProps = {
        docsLink: {href: DEPLOYMENTS_DOCUMENTATION_URL, label: 'How deployments work'},
        note: environmentScopeNote,
    };

    const journeySteps = journey && (
        <JourneySteps aria-label="Getting started" steps={getAutomationJourneySteps(journey.stepStatuses)} />
    );

    if (stage === 'createProject' || stage === 'buildWorkflow') {
        return (
            <EmptyState
                {...sharedProps}
                actions={<ProjectsStepButton onClick={() => captureAction('go_to_projects')} />}
                description="Deployments run a published version of a project. Create a project and build a workflow first."
                icon={<Layers3Icon />}
                title="Nothing to deploy yet"
            >
                {journeySteps}
            </EmptyState>
        );
    }

    if (stage === 'publish') {
        return (
            <EmptyState
                {...sharedProps}
                actions={
                    <ProjectsStepButton
                        nextProject={journey?.nextProject}
                        onClick={() => captureAction(journey?.nextProject ? 'open_project' : 'go_to_projects')}
                    />
                }
                description={getPublishDescription(journey?.nextProject)}
                icon={<SendIcon />}
                title="Publish a project to deploy it"
            >
                {journeySteps}
            </EmptyState>
        );
    }

    return (
        <EmptyState
            {...sharedProps}
            actions={
                <ProjectDeploymentDialog
                    onSuccess={onDeploymentCreated}
                    projectDeployment={{environmentId: currentEnvironmentId} as ProjectDeployment}
                    redirectOnSubmit={false}
                    triggerNode={
                        <Button label="Create Deployment" onClick={() => captureAction('create_deployment')} />
                    }
                />
            }
            description="A deployment runs a published version of a project. Choose the version, pick which workflows to enable, and connect their accounts."
            icon={<Layers3Icon />}
            title="Create your first deployment"
        >
            {journeySteps}
        </EmptyState>
    );
};

export default ProjectDeploymentsEmptyState;
