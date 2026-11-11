import {EmbeddedGettingStartedQuery} from '@/shared/middleware/graphql';

type EmbeddedGettingStartedStatusType = EmbeddedGettingStartedQuery['embeddedGettingStarted'];

export interface EmbeddedGettingStartedSubStepI {
    done: boolean;
    hint: string;
    href: string;
    id: string;
    label: string;
}

const CONNECTED_USERS_HREF = '/embedded/connected-users';

const INTEGRATIONS_HREF = '/embedded/integrations';

export function getIntegrationSubSteps(status: EmbeddedGettingStartedStatusType): EmbeddedGettingStartedSubStepI[] {
    return [
        {
            done: status.integrationCreated,
            hint: 'Integrations → New Integration → pick HTTP Client to try it out, or the app your users connect',
            href: INTEGRATIONS_HREF,
            id: 'create',
            label: 'Create an integration',
        },
        ...(status.integrationHasWorkflows
            ? [
                  {
                      done: status.integrationPublished,
                      hint: 'Integration menu (⋮) → Publish',
                      href: INTEGRATIONS_HREF,
                      id: 'publish',
                      label: 'Publish a version',
                  },
              ]
            : []),
        {
            done: status.integrationConfigurationCreated,
            hint: 'Integration menu (⋮) → New Configuration',
            href: INTEGRATIONS_HREF,
            id: 'configure',
            label: 'Create a configuration in Development',
        },
        ...(status.integrationHasWorkflows ||
        (status.integrationConfigurationCreated && !status.integrationConfigurationEnabled)
            ? [
                  {
                      done: status.integrationConfigurationEnabled,
                      hint: 'Expand the integration → Configurations tab → switch it on',
                      href: INTEGRATIONS_HREF,
                      id: 'enable',
                      label: 'Enable the configuration',
                  },
              ]
            : []),
    ];
}

export function getTestUserSubSteps(status: EmbeddedGettingStartedStatusType): EmbeddedGettingStartedSubStepI[] {
    return [
        {
            done: status.testUserAdded,
            hint: "Connected Users → Add Connected User. Use your app's user ID as the External ID.",
            href: CONNECTED_USERS_HREF,
            id: 'add',
            label: 'Add a connected user',
        },
        {
            done: status.testUserConnected,
            hint: 'Open the user → New Integration, pick your configuration and enter their credentials',
            href: CONNECTED_USERS_HREF,
            id: 'provision',
            label: 'Provision your integration and connect their account',
        },
    ];
}
