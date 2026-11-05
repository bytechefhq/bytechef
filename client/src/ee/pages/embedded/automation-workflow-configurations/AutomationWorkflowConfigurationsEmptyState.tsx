import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps, {type JourneyStepI, type JourneyStepStatusType} from '@/components/JourneySteps/JourneySteps';
import {EMBEDDED_AUTOMATION_CONFIGURATIONS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import {SlidersHorizontalIcon} from 'lucide-react';
import {useMemo} from 'react';

const getStepStatus = (complete: boolean, current: boolean): JourneyStepStatusType =>
    complete ? 'complete' : current ? 'current' : 'upcoming';

interface AutomationWorkflowConfigurationsEmptyStateProps {
    hasProject: boolean;
    hasPublishedProject: boolean;
    onCreateConfiguration?: () => void;
    onGoToAutomations: () => void;
}

const AutomationWorkflowConfigurationsEmptyState = ({
    hasProject,
    hasPublishedProject,
    onCreateConfiguration,
    onGoToAutomations,
}: AutomationWorkflowConfigurationsEmptyStateProps) => {
    const environmentScopeNote = useEnvironmentScopeNote('Configurations');

    const steps: JourneyStepI[] = useMemo(() => {
        const goToAutomationsButton = <Button label="Go to Automations" onClick={onGoToAutomations} size="sm" />;

        const createProjectStatus = getStepStatus(hasProject, true);
        const publishVersionStatus = getStepStatus(hasPublishedProject, hasProject);
        const createConfigurationStatus = getStepStatus(false, hasPublishedProject);

        return [
            {
                ...(createProjectStatus === 'current' && {
                    action: goToAutomationsButton,
                    description: 'A project holds the automation workflows your users run.',
                }),
                id: 'createProject',
                label: 'Create a project',
                status: createProjectStatus,
            },
            {
                ...(publishVersionStatus === 'current' && {
                    action: goToAutomationsButton,
                    description:
                        'Publish the project in the Development environment to get a version you can configure.',
                }),
                id: 'publishVersion',
                label: 'Publish a version',
                status: publishVersionStatus,
            },
            {
                ...(createConfigurationStatus === 'current' && {
                    ...(onCreateConfiguration && {
                        action: <Button label="Create Configuration" onClick={onCreateConfiguration} size="sm" />,
                    }),
                    description: 'Pick a project and a published version, then name the configuration.',
                }),
                id: 'createConfiguration',
                label: 'Create a configuration',
                status: createConfigurationStatus,
            },
        ];
    }, [hasProject, hasPublishedProject, onCreateConfiguration, onGoToAutomations]);

    return (
        <EmptyState
            description="A configuration runs a published version of an automation project for connected users in this environment. Three steps to get there:"
            docsLink={{
                href: EMBEDDED_AUTOMATION_CONFIGURATIONS_DOCUMENTATION_URL,
                label: 'How configurations work',
            }}
            icon={<SlidersHorizontalIcon />}
            note={environmentScopeNote}
            title="Create your first configuration"
        >
            <JourneySteps aria-label="Getting started" className="max-w-md" orientation="vertical" steps={steps} />
        </EmptyState>
    );
};

export default AutomationWorkflowConfigurationsEmptyState;
