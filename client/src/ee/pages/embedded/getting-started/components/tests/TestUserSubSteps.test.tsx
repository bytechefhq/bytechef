import {useConnectedUserSheetStore} from '@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import {EmbeddedGettingStartedQuery} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {cleanup, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import TestUserSubSteps from '../TestUserSubSteps';

type StatusType = EmbeddedGettingStartedQuery['embeddedGettingStarted'];

vi.mock('@/ee/pages/embedded/connected-users/components/ConnectedUserCreateDialog', () => ({
    default: () => <div data-testid="connected-user-create-dialog" />,
}));

vi.mock('@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheet', () => ({
    default: () => null,
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
                <TestUserSubSteps status={status} />
            </MemoryRouter>
        </QueryClientProvider>
    );

describe('TestUserSubSteps', () => {
    beforeEach(() => {
        useConnectedUserSheetStore.setState({connectedUserId: 0, connectedUserSheetOpen: false});
    });

    afterEach(() => {
        cleanup();
    });

    it('opens the new connected user dialog in place', async () => {
        renderSubSteps(baseStatus);

        await userEvent.click(screen.getByRole('button', {name: 'Add a connected user'}));

        expect(screen.getByTestId('connected-user-create-dialog')).toBeInTheDocument();
    });

    it('links provisioning while there is no connected user', () => {
        renderSubSteps(baseStatus);

        expect(
            screen.getByRole('link', {name: 'Provision your integration and connect their account'})
        ).toHaveAttribute('href', '/embedded/connected-users');
    });

    it("opens the test user's panel to provision the integration", async () => {
        renderSubSteps({...baseStatus, testUserAdded: true, testUserId: 42});

        await userEvent.click(
            screen.getByRole('button', {name: 'Provision your integration and connect their account'})
        );

        expect(useConnectedUserSheetStore.getState()).toMatchObject({
            connectedUserId: 42,
            connectedUserSheetOpen: true,
        });
    });
});
