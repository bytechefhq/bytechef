import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps, {type JourneyStepI, type JourneyStepStatusType} from '@/components/JourneySteps/JourneySteps';
import {EMBEDDED_INSTANCE_CONFIGURATIONS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
import IntegrationInstanceConfigurationDialog from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog';
import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {Settings2Icon} from 'lucide-react';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

const getStepStatus = (complete: boolean, current: boolean): JourneyStepStatusType =>
    complete ? 'complete' : current ? 'current' : 'upcoming';

/*
 * An instance configuration deploys a published integration version to this environment, so a new user first needs an
 * integration with a published version. The steps read both from the integration list, which is not environment scoped.
 */
const IntegrationInstanceConfigurationsEmptyState = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const navigate = useNavigate();

    const environmentScopeNote = useEnvironmentScopeNote('Instance configurations');

    const {data: integrations} = useGetIntegrationsQuery({includeAllFields: false});

    const hasIntegration = !!integrations && integrations.length > 0;
    const hasPublishedIntegration = !!integrations?.some((integration) => !!integration.lastPublishedDate);

    const steps: JourneyStepI[] = useMemo(() => {
        const goToIntegrationsButton = (
            <Button label="Go to Integrations" onClick={() => void navigate('/embedded/integrations')} size="sm" />
        );

        const createIntegrationStatus = getStepStatus(hasIntegration, true);
        const publishVersionStatus = getStepStatus(hasPublishedIntegration, hasIntegration);
        const createConfigurationStatus = getStepStatus(false, hasPublishedIntegration);

        return [
            {
                ...(createIntegrationStatus === 'current' && {
                    action: goToIntegrationsButton,
                    description: 'An integration holds the workflows your users turn on.',
                }),
                id: 'createIntegration',
                label: 'Create an integration',
                status: createIntegrationStatus,
            },
            {
                ...(publishVersionStatus === 'current' && {
                    action: goToIntegrationsButton,
                    description: 'Publishing creates a read-only version you can configure.',
                }),
                id: 'publishVersion',
                label: 'Publish a version',
                status: publishVersionStatus,
            },
            {
                ...(createConfigurationStatus === 'current' && {
                    action: (
                        <IntegrationInstanceConfigurationDialog
                            integrationInstanceConfiguration={
                                {
                                    environmentId: currentEnvironmentId,
                                } as IntegrationInstanceConfiguration
                            }
                            triggerNode={<Button label="Create Instance Configuration" size="sm" />}
                        />
                    ),
                    description: 'Pick a published version and set it up for this environment.',
                }),
                id: 'createInstanceConfiguration',
                label: 'Create an instance configuration',
                status: createConfigurationStatus,
            },
        ];
    }, [currentEnvironmentId, hasIntegration, hasPublishedIntegration, navigate]);

    return (
        <EmptyState
            description="An instance configuration makes a published integration version available to your users in this environment, with its settings and connection. Three steps to get there:"
            docsLink={{
                href: EMBEDDED_INSTANCE_CONFIGURATIONS_DOCUMENTATION_URL,
                label: 'How instance configurations work',
            }}
            icon={<Settings2Icon />}
            note={environmentScopeNote}
            title="Create your first instance configuration"
        >
            {integrations && (
                <JourneySteps aria-label="Getting started" className="max-w-md" orientation="vertical" steps={steps} />
            )}
        </EmptyState>
    );
};

export default IntegrationInstanceConfigurationsEmptyState;
