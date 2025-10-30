import {UserI} from '@/shared/models/user.model';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {PostHog} from 'posthog-js';
import {useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

export interface AnalyticsI {
    captureComponentUsed(name: string, actionName?: string, triggerName?: string): void;
    captureIntegrationCreated(): void;
    captureIntegrationInstanceConfigurationCreated(): void;
    captureIntegrationInstanceConfigurationEnabled(): void;
    captureIntegrationPublished(): void;
    captureIntegrationWorkflowCreated(): void;
    captureIntegrationWorkflowImported(): void;
    captureIntegrationWorkflowTested(): void;
    captureProjectCreated(): void;
    captureProjectDeploymentCreated(): void;
    captureProjectDeploymentEnabled(): void;
    captureProjectPublished(): void;
    captureProjectWorkflowCreated(): void;
    captureProjectWorkflowImported(): void;
    captureProjectWorkflowTested(): void;
    captureUserSignedUp(email: string): void;
    identify(account: UserI): void;
    reset(): void;
}

export const useAnalytics = (): AnalyticsI => {
    const identifyRef = useRef(false);
    const posthogRef = useRef<PostHog | null>(null);

    const {application} = useApplicationInfoStore(
        useShallow((state) => ({
            analytics: state.analytics,
            application: state.application,
        }))
    );

    const getPostHog = async () => {
        if (!posthogRef.current) {
            try {
                const posthogModule = await import('posthog-js');

                posthogRef.current = posthogModule.default;
            } catch (error) {
                console.warn('PostHog failed to load:', error);

                return null;
            }
        }

        return posthogRef.current;
    };

    const captureEvent = async (eventName: string, properties?: Record<string, unknown>) => {
        const posthog = await getPostHog();

        if (posthog) {
            posthog.capture(eventName, properties);
        }
    };

    return {
        captureComponentUsed: (componentName: string, actionName?: string, triggerName?: string) =>
            captureEvent('component_used', {actionName, componentName, triggerName}),
        captureIntegrationCreated: () => captureEvent('integration_created'),
        captureIntegrationInstanceConfigurationCreated: () =>
            captureEvent('integration_instance_configuration_created'),
        captureIntegrationInstanceConfigurationEnabled: () =>
            captureEvent('integration_instance_configuration_enabled'),
        captureIntegrationPublished: () => captureEvent('integration_published'),
        captureIntegrationWorkflowCreated: () => captureEvent('integration_workflow_created'),
        captureIntegrationWorkflowImported: () => captureEvent('integration_workflow_created', {imported: true}),
        captureIntegrationWorkflowTested: () => captureEvent('integration_workflow_tested'),
        captureProjectCreated: () => captureEvent('project_created'),
        captureProjectDeploymentCreated: () => captureEvent('project_deployment_created'),
        captureProjectDeploymentEnabled: () => captureEvent('project_deployment_enabled'),
        captureProjectPublished: () => captureEvent('project_published'),
        captureProjectWorkflowCreated: () => captureEvent('project_workflow_created'),
        captureProjectWorkflowImported: () => captureEvent('project_workflow_created', {imported: true}),
        captureProjectWorkflowTested: () => captureEvent('project_workflow_tested'),
        captureUserSignedUp: (email: string) => captureEvent('user_signed_up', {email}),
        identify: async (account: UserI) => {
            if (identifyRef.current) {
                return;
            }

            identifyRef.current = true;

            const posthog = await getPostHog();

            if (posthog) {
                posthog.identify(account.uuid, {
                    edition: application?.edition,
                    email: account.email,
                    name: `${account.firstName} ${account.lastName}`,
                });
            }
        },
        reset: async () => {
            const posthog = await getPostHog();

            if (posthog) {
                posthog.reset();
            }

            identifyRef.current = false;
        },
    };
};
