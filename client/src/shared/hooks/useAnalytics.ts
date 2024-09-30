import {UserI} from '@/shared/models/user.model';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {usePostHog} from 'posthog-js/react';

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
    captureProjectInstanceCreated(): void;
    captureProjectInstanceEnabled(): void;
    captureProjectPublished(): void;
    captureProjectWorkflowCreated(): void;
    captureProjectWorkflowImported(): void;
    captureProjectWorkflowTested(): void;
    captureUserSignedUp(email: string): void;
    identify(account: UserI): void;
    reset(): void;
}

export const useAnalytics = (): AnalyticsI => {
    const {application} = useApplicationInfoStore();

    const posthog = usePostHog();

    return {
        captureComponentUsed: (componentName: string, actionName?: string, triggerName?: string) => {
            posthog.capture('component_used', {actionName, componentName, triggerName});
        },
        captureIntegrationCreated: () => {
            posthog.capture('integration_created');
        },
        captureIntegrationInstanceConfigurationCreated: () => {
            posthog.capture('integration_instance_configuration_created');
        },
        captureIntegrationInstanceConfigurationEnabled: () => {
            posthog.capture('integration_instance_configuration_enabled');
        },
        captureIntegrationPublished: () => {
            posthog.capture('integration_published');
        },
        captureIntegrationWorkflowCreated: () => {
            posthog.capture('integration_workflow_created');
        },
        captureIntegrationWorkflowImported: () => {
            posthog.capture('integration_workflow_created', {imported: true});
        },
        captureIntegrationWorkflowTested: () => {
            posthog.capture('integration_workflow_tested');
        },
        captureProjectCreated: () => {
            posthog.capture('project_created');
        },
        captureProjectInstanceCreated: () => {
            posthog.capture('project_instance_created');
        },
        captureProjectInstanceEnabled: () => {
            posthog.capture('project_instance_enabled');
        },
        captureProjectPublished: () => {
            posthog.capture('project_published');
        },
        captureProjectWorkflowCreated: () => {
            posthog.capture('project_workflow_created');
        },
        captureProjectWorkflowImported: () => {
            posthog.capture('project_workflow_created', {imported: true});
        },
        captureProjectWorkflowTested: () => {
            posthog.capture('project_workflow_tested');
        },
        captureUserSignedUp(email: string): void {
            posthog.capture('user_signed_up', {email});
        },
        identify: (account: UserI) => {
            posthog?.identify(account.uuid, {
                edition: application?.edition,
                email: account.email,
                name: `${account.firstName} ${account.lastName}`,
            });
            // posthog?.group('company', account);
        },
        reset: () => {
            posthog.reset();
        },
    };
};
