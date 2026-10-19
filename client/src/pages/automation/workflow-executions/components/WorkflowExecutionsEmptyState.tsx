import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps from '@/components/JourneySteps/JourneySteps';
import PageLoader from '@/components/PageLoader';
import ProjectsStepButton from '@/pages/automation/journey/ProjectsStepButton';
import {
    WORKFLOW_EXECUTIONS_DOCUMENTATION_URL,
    getAutomationJourneySteps,
} from '@/pages/automation/journey/automationJourneySteps';
import {
    type AutomationJourneyI,
    type AutomationJourneyStageType,
} from '@/pages/automation/journey/getAutomationJourney';
import {useAutomationJourney} from '@/pages/automation/journey/useAutomationJourney';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import {ActivityIcon, ClockIcon, PowerIcon, RefreshCwIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

interface WorkflowExecutionsEmptyStateProps {
    onRefresh: () => void;
}

const CURRENT_STEP_DESCRIPTIONS: Partial<Record<AutomationJourneyStageType, string>> = {
    buildWorkflow: 'Add a workflow to a project and test it in the editor.',
    createProject: 'Projects group related workflows.',
    deploy: 'Pick a published version and enable its workflows.',
    publish: 'Publishing creates a read-only version you can deploy.',
};

/*
 * Executions are the last stop of the journey, so a new user lands on an empty page with nothing to do here.
 * Show what is left before a first run, with the next step's action, or what the page is waiting for.
 */
const WorkflowExecutionsEmptyState = ({onRefresh}: WorkflowExecutionsEmptyStateProps) => {
    const navigate = useNavigate();

    const {captureEmptyStateAction} = useAnalytics();
    const environmentScopeNote = useEnvironmentScopeNote('Executions');
    const {isLoading, journey} = useAutomationJourney();

    if (isLoading) {
        return <PageLoader loading />;
    }

    const stage = journey?.stage;

    const captureAction = (action: string) => captureEmptyStateAction('executions', action, stage);

    const goToDeployments = (action: string) => {
        captureAction(action);

        navigate('/automation/deployments');
    };

    const sharedProps = {
        docsLink: {href: WORKFLOW_EXECUTIONS_DOCUMENTATION_URL, label: 'How executions work'},
        note: environmentScopeNote,
    };

    if (stage === 'enable') {
        return (
            <EmptyState
                {...sharedProps}
                actions={<Button label="Go to Deployments" onClick={() => goToDeployments('go_to_deployments')} />}
                description="You have a deployment, but none of its workflows are enabled. Enabled workflows start when their trigger fires."
                icon={<PowerIcon />}
                title="Enable your deployment"
            />
        );
    }

    if (stage === 'live') {
        return (
            <EmptyState
                {...sharedProps}
                actions={
                    <>
                        <Button
                            icon={<RefreshCwIcon />}
                            label="Refresh"
                            onClick={() => {
                                captureAction('refresh');

                                onRefresh();
                            }}
                            variant="outline"
                        />

                        <Button
                            label="View deployments"
                            onClick={() => goToDeployments('view_deployments')}
                            variant="outline"
                        />
                    </>
                }
                description="Your deployment is enabled. Runs appear here when a trigger fires, for example on a schedule or an incoming webhook. Test runs from the workflow editor aren't listed here."
                icon={<ClockIcon />}
                title="Waiting for the first run"
            />
        );
    }

    return (
        <EmptyState
            {...sharedProps}
            description={
                journey
                    ? 'Runs show up here after a deployed workflow is triggered. Four steps to get there:'
                    : 'Runs show up here after a deployed workflow is triggered.'
            }
            icon={<ActivityIcon />}
            title="No workflow runs yet"
        >
            {journey && (
                <JourneySteps
                    aria-label="Getting started"
                    className="max-w-md"
                    orientation="vertical"
                    steps={getAutomationJourneySteps(journey.stepStatuses, {
                        action: (
                            <CurrentStepAction
                                journey={journey}
                                onDeploymentsClick={() => goToDeployments('go_to_deployments')}
                                onProjectsClick={captureAction}
                            />
                        ),
                        description: CURRENT_STEP_DESCRIPTIONS[journey.stage],
                    })}
                />
            )}
        </EmptyState>
    );
};

interface CurrentStepActionProps {
    journey: AutomationJourneyI;
    onDeploymentsClick: () => void;
    onProjectsClick: (action: string) => void;
}

const CurrentStepAction = ({journey, onDeploymentsClick, onProjectsClick}: CurrentStepActionProps) => {
    if (journey.stage === 'deploy') {
        return <Button label="Go to Deployments" onClick={onDeploymentsClick} size="sm" />;
    }

    return (
        <ProjectsStepButton
            nextProject={journey.nextProject}
            onClick={() => onProjectsClick(journey.nextProject ? 'open_project' : 'go_to_projects')}
            size="sm"
        />
    );
};

export default WorkflowExecutionsEmptyState;
