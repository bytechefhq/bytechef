import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EmbeddedGettingStartedQuery} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {cleanup, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationSubSteps from '../IntegrationSubSteps';

type StatusType = EmbeddedGettingStartedQuery['embeddedGettingStarted'];

const hoisted = vi.hoisted(() => ({
    captureIntegrationInstanceConfigurationEnabledMock: vi.fn(),
    configurationDialogMock: vi.fn(),
    enableMutateMock: vi.fn(),
    enableMutationState: {isPending: false},
    integrationDialogMock: vi.fn(),
    publishDialogMock: vi.fn(),
    useGetIntegrationQueryMock: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/integrations/components/IntegrationDialog', () => ({
    default: (props: {integration?: {componentName?: string; id?: number; name?: string}}) => {
        hoisted.integrationDialogMock(props);

        return <div data-testid="integration-dialog" />;
    },
}));

vi.mock('@/ee/pages/embedded/integrations/components/IntegrationPublishDialog', () => ({
    default: (props: {integration: {id: number}}) => {
        hoisted.publishDialogMock(props);

        return <div data-testid="integration-publish-dialog" />;
    },
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog',
    () => ({
        default: (props: {integrationInstanceConfiguration: {environmentId: number; integrationId: number}}) => {
            hoisted.configurationDialogMock(props);

            return <div data-testid="integration-instance-configuration-dialog" />;
        },
    })
);

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceConfigurations.mutations', () => ({
    useEnableIntegrationInstanceConfigurationMutation: () => ({
        isPending: hoisted.enableMutationState.isPending,
        mutate: hoisted.enableMutateMock,
    }),
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationQuery: hoisted.useGetIntegrationQueryMock,
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({
        captureIntegrationInstanceConfigurationEnabled: hoisted.captureIntegrationInstanceConfigurationEnabledMock,
    }),
}));

const baseStatus: StatusType = {
    apiBaseUrl: null,
    apiKeyCreated: false,
    completed: false,
    completionType: null,
    componentName: null,
    componentVersion: null,
    firstCallMade: false,
    integrationConfigurationCreated: false,
    integrationConfigurationEnabled: false,
    integrationCreated: false,
    integrationHasWorkflows: true,
    integrationId: null,
    integrationInstanceConfigurationId: null,
    integrationPublished: false,
    lastFailedCall: null,
    testUserAdded: false,
    testUserConnected: false,
    testUserExternalId: null,
    testUserId: null,
};

const renderSubSteps = (status: StatusType) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter>
                <IntegrationSubSteps status={status} />
            </MemoryRouter>
        </QueryClientProvider>
    );

describe('IntegrationSubSteps', () => {
    beforeEach(() => {
        hoisted.enableMutationState.isPending = false;
        hoisted.useGetIntegrationQueryMock.mockReturnValue({data: undefined});
    });

    afterEach(() => {
        cleanup();
        vi.clearAllMocks();
    });

    it('opens the new integration dialog in place', async () => {
        renderSubSteps(baseStatus);

        await userEvent.click(screen.getByRole('button', {name: 'Create an integration'}));

        expect(screen.getByTestId('integration-dialog')).toBeInTheDocument();
        expect(hoisted.integrationDialogMock).toHaveBeenCalledWith(
            expect.objectContaining({integration: {componentName: 'httpClient', name: 'HTTP Client'}})
        );
    });

    it('links the steps that have no integration to act on yet', () => {
        renderSubSteps(baseStatus);

        expect(screen.getByRole('link', {name: 'Publish a version'})).toHaveAttribute('href', '/embedded/integrations');
        expect(screen.getByRole('link', {name: 'Create a configuration in Development'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Enable the configuration'})).toBeInTheDocument();
    });

    it('opens the publish dialog for the target integration', async () => {
        const integration = {id: 12, name: 'GitHub'};

        hoisted.useGetIntegrationQueryMock.mockImplementation((id: number, initialData: unknown, enabled: boolean) => ({
            data: enabled ? integration : undefined,
        }));

        renderSubSteps({...baseStatus, integrationCreated: true, integrationId: 12});

        await userEvent.click(screen.getByRole('button', {name: 'Publish a version'}));

        expect(hoisted.useGetIntegrationQueryMock).toHaveBeenLastCalledWith(12, undefined, true);
        expect(hoisted.publishDialogMock).toHaveBeenLastCalledWith(expect.objectContaining({integration}));
    });

    it('opens a Development configuration dialog for the published integration', async () => {
        renderSubSteps({...baseStatus, integrationCreated: true, integrationId: 12, integrationPublished: true});

        await userEvent.click(screen.getByRole('button', {name: 'Create a configuration in Development'}));

        expect(hoisted.configurationDialogMock).toHaveBeenLastCalledWith(
            expect.objectContaining({
                integrationInstanceConfiguration: {environmentId: DEVELOPMENT_ENVIRONMENT, integrationId: 12},
            })
        );
    });

    it('opens the configuration dialog for an unpublished integration without workflows', async () => {
        renderSubSteps({...baseStatus, integrationCreated: true, integrationHasWorkflows: false, integrationId: 12});

        expect(screen.queryByText('Publish a version')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Create a configuration in Development'}));

        expect(hoisted.configurationDialogMock).toHaveBeenLastCalledWith(
            expect.objectContaining({
                integrationInstanceConfiguration: {environmentId: DEVELOPMENT_ENVIRONMENT, integrationId: 12},
            })
        );
    });

    it('enables the target configuration in place', async () => {
        renderSubSteps({
            ...baseStatus,
            integrationConfigurationCreated: true,
            integrationCreated: true,
            integrationId: 12,
            integrationInstanceConfigurationId: 30,
            integrationPublished: true,
        });

        await userEvent.click(screen.getByRole('button', {name: 'Enable the configuration'}));

        expect(hoisted.enableMutateMock).toHaveBeenCalledWith({enable: true, id: 30});
    });

    it('does not enable the configuration again while enabling is in progress', async () => {
        hoisted.enableMutationState.isPending = true;

        renderSubSteps({
            ...baseStatus,
            integrationConfigurationCreated: true,
            integrationCreated: true,
            integrationId: 12,
            integrationInstanceConfigurationId: 30,
            integrationPublished: true,
        });

        await userEvent.click(screen.getByRole('button', {name: 'Enable the configuration'}));

        expect(hoisted.enableMutateMock).not.toHaveBeenCalled();
    });
});
