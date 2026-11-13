import {TooltipProvider} from '@/components/ui/tooltip';
import {useConnectedUserSheetStore} from '@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import {EmbeddedGettingStartedQuery} from '@/shared/middleware/graphql';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, cleanup, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import EmbeddedGettingStarted from '../EmbeddedGettingStarted';

type StatusType = EmbeddedGettingStartedQuery['embeddedGettingStarted'];

const hoisted = vi.hoisted(() => ({
    dismissMutateMock: vi.fn(),
    useDismissEmbeddedGettingStartedMutationMock: vi.fn(),
    useEmbeddedGettingStartedQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    PlatformType: {Automation: 'AUTOMATION', Embedded: 'EMBEDDED'},
    useApiKeysQuery: vi.fn(),
    useCreateApiKeyMutation: vi.fn(),
    useDeleteApiKeyMutation: vi.fn(),
    useDismissEmbeddedGettingStartedMutation: hoisted.useDismissEmbeddedGettingStartedMutationMock,
    useEmbeddedGettingStartedQuery: hoisted.useEmbeddedGettingStartedQueryMock,
    useEnvironmentsQuery: () => ({data: undefined}),
    useUpdateApiKeyMutation: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/integrations/components/IntegrationDialog', () => ({default: () => null}));

vi.mock('@/ee/pages/embedded/integrations/components/IntegrationPublishDialog', () => ({default: () => null}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog',
    () => ({default: () => null})
);

vi.mock('@/ee/pages/embedded/connected-users/components/ConnectedUserCreateDialog', () => ({default: () => null}));

vi.mock('@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheet', () => ({
    default: () => null,
}));

vi.mock('@/shared/components/api-keys/components/ApiKeyDialog', () => ({
    default: ({triggerNode}: {triggerNode?: ReactNode}) => <div data-testid="api-key-dialog">{triggerNode}</div>,
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

const readyStatus: StatusType = {
    ...baseStatus,
    apiKeyCreated: true,
    integrationConfigurationCreated: true,
    integrationConfigurationEnabled: true,
    integrationCreated: true,
    integrationHasWorkflows: true,
    integrationPublished: true,
    testUserAdded: true,
    testUserConnected: true,
    testUserExternalId: 'user-42',
};

let queryClient: QueryClient;

const getSetupSteps = () => {
    const setupSteps = screen.getByRole('list', {name: 'Setup steps'});

    return within(setupSteps)
        .getAllByRole('listitem')
        .filter((listItem) => listItem.parentElement === setupSteps);
};

const mockStatus = (status: StatusType) => {
    hoisted.useEmbeddedGettingStartedQueryMock.mockReturnValue({
        data: {embeddedGettingStarted: status},
        error: null,
        isLoading: false,
    });
};

const renderPage = () =>
    render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <MemoryRouter>
                    <EmbeddedGettingStarted />
                </MemoryRouter>
            </TooltipProvider>
        </QueryClientProvider>
    );

const getRefetchInterval = (status: StatusType) => {
    const options = hoisted.useEmbeddedGettingStartedQueryMock.mock.lastCall?.[1];

    return options.refetchInterval({state: {data: {embeddedGettingStarted: status}}});
};

describe('EmbeddedGettingStarted', () => {
    beforeEach(() => {
        queryClient = new QueryClient();

        vi.spyOn(queryClient, 'invalidateQueries');

        hoisted.dismissMutateMock.mockReset();
        hoisted.useDismissEmbeddedGettingStartedMutationMock.mockReset();
        hoisted.useDismissEmbeddedGettingStartedMutationMock.mockReturnValue({
            isPending: false,
            mutate: hoisted.dismissMutateMock,
        });
        hoisted.useEmbeddedGettingStartedQueryMock.mockReset();

        mockStatus(baseStatus);

        environmentStore.setState({currentEnvironmentId: 0});

        useConnectedUserSheetStore.setState({connectedUserId: 0, connectedUserSheetOpen: false});
    });

    afterEach(() => {
        cleanup();
    });

    it('renders the four steps with statuses from the query', () => {
        mockStatus({...baseStatus, apiKeyCreated: true});

        renderPage();

        const steps = getSetupSteps();

        expect(steps).toHaveLength(4);
        expect(steps[0]).toHaveTextContent('Completed:');
        expect(steps[1]).toHaveAttribute('aria-current', 'step');
        expect(steps[2]).not.toHaveAttribute('aria-current');
        expect(screen.getByText('Key created')).toBeInTheDocument();
        expect(hoisted.useEmbeddedGettingStartedQueryMock).toHaveBeenLastCalledWith(
            {},
            expect.objectContaining({enabled: true})
        );
    });

    it('shows the create API key button when no key exists', () => {
        renderPage();

        expect(screen.getByRole('button', {name: 'Create API key'})).toBeInTheDocument();
        expect(screen.queryByText('Key created')).not.toBeInTheDocument();
    });

    it('lists the integration and test user sub-steps with links for the ones left to do', () => {
        mockStatus({...baseStatus, apiKeyCreated: true, integrationCreated: true, testUserAdded: true});

        renderPage();

        const integrationSetup = screen.getByRole('list', {name: 'Integration setup'});

        expect(within(integrationSetup).queryByRole('link', {name: 'Create an integration'})).not.toBeInTheDocument();
        expect(within(integrationSetup).getByText('Create an integration')).toBeInTheDocument();
        expect(within(integrationSetup).getByRole('link', {name: 'Publish a version'})).toHaveAttribute(
            'href',
            '/embedded/integrations'
        );

        const testUserSetup = screen.getByRole('list', {name: 'Test user setup'});

        expect(within(testUserSetup).queryByRole('link', {name: 'Add a connected user'})).not.toBeInTheDocument();
        expect(
            within(testUserSetup).getByRole('link', {name: 'Provision your integration and connect their account'})
        ).toHaveAttribute('href', '/embedded/connected-users');
        expect(screen.queryByText('Published and enabled')).not.toBeInTheDocument();
    });

    it('collapses the completed integration and test user steps and expands them on click', async () => {
        mockStatus(readyStatus);

        renderPage();

        expect(screen.queryByText('user-42')).not.toBeInTheDocument();
        expect(screen.queryByText('Publish a version')).not.toBeInTheDocument();
        expect(screen.getByText('User connected')).toBeInTheDocument();

        const testUserToggle = screen.getByRole('button', {name: /Connect a test user/});

        expect(testUserToggle).toHaveAttribute('aria-expanded', 'false');

        await userEvent.click(testUserToggle);

        expect(testUserToggle).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByText('user-42')).toBeInTheDocument();
        expect(within(getSetupSteps()[2]).getByRole('button', {name: 'Copy to clipboard'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: /Set up your first integration/}));

        expect(screen.getByText('Publish a version')).toBeInTheDocument();
    });

    it('renders the curl call muted and still listens before steps 1-3', () => {
        renderPage();

        const curl = screen.getByTestId('first-call-curl');

        expect(curl).toHaveTextContent(`${window.location.origin}/api/embedded/v1/EXTERNAL_USER_ID/components/`);
        expect(curl).toHaveTextContent('X-Environment: DEVELOPMENT');
        expect(curl.parentElement).toHaveClass('opacity-50');
        expect(screen.getByText(/replace ACTION_NAME and the input/)).toBeInTheDocument();
        expect(screen.getByText('Listening for the call')).toBeInTheDocument();
        expect(getRefetchInterval(baseStatus)).toBe(5000);
        expect(screen.getByTestId('listening-indicator')).not.toHaveClass('animate-pulse');
    });

    it('uses the API base URL built from the public URL setting', () => {
        mockStatus({...baseStatus, apiBaseUrl: 'https://app.example.com/api/embedded/v1'});

        renderPage();

        expect(screen.getByTestId('first-call-curl')).toHaveTextContent(
            "curl -X POST 'https://app.example.com/api/embedded/v1/"
        );
    });

    it('leaves for the configurations page outside Development', () => {
        environmentStore.setState({currentEnvironmentId: 1});

        render(
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/embedded/getting-started']}>
                    <Routes>
                        <Route element={<EmbeddedGettingStarted />} path="/embedded/getting-started" />

                        <Route element={<div>Configurations page</div>} path="/embedded/configurations" />
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>
        );

        expect(screen.getByText('Configurations page')).toBeInTheDocument();
        expect(hoisted.useEmbeddedGettingStartedQueryMock).toHaveBeenLastCalledWith(
            {},
            expect.objectContaining({enabled: false})
        );
    });

    it('shows listening box and polls every 5 seconds once steps 1-3 are done', () => {
        mockStatus(readyStatus);

        renderPage();

        const curl = screen.getByTestId('first-call-curl');

        expect(curl).toHaveTextContent('/user-42/components/');
        expect(curl.parentElement).not.toHaveClass('opacity-50');
        expect(screen.getByText('Listening for the call')).toBeInTheDocument();
        expect(getRefetchInterval(readyStatus)).toBe(5000);
        expect(screen.getByTestId('listening-indicator')).toHaveClass('animate-pulse');
        expect(getRefetchInterval({...readyStatus, firstCallMade: true})).toBe(false);
    });

    it('builds a ready-to-run HTTP Client call for the configured integration', () => {
        mockStatus({...readyStatus, componentName: 'httpClient', componentVersion: 1});

        renderPage();

        expect(screen.getByTestId('first-call-curl')).toHaveTextContent(
            '/user-42/components/httpClient/versions/1/actions/get'
        );
        expect(
            screen.getByText('Set BYTECHEF_API_KEY to your API key, then run this in a terminal.')
        ).toBeInTheDocument();
    });

    it('shows the latest failed call', () => {
        mockStatus({
            ...readyStatus,
            lastFailedCall: {createdDate: 1700000000000, errorMessage: 'Connection missing', status: 'ERROR'},
        });

        renderPage();

        expect(screen.getByTestId('last-failed-call')).toHaveTextContent('ERROR');
        expect(screen.getByTestId('last-failed-call')).toHaveTextContent('Connection missing');
        expect(screen.getByRole('link', {name: 'View external invocations'})).toHaveAttribute(
            'href',
            '/embedded/external-invocations'
        );
    });

    it('collapses an in-progress step on click and expands it again', async () => {
        renderPage();

        const integrationToggle = screen.getByRole('button', {name: /Set up your first integration/});

        expect(integrationToggle).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByText('Publish a version')).toBeInTheDocument();

        await userEvent.click(integrationToggle);

        expect(integrationToggle).toHaveAttribute('aria-expanded', 'false');
        expect(screen.queryByText('Publish a version')).not.toBeInTheDocument();

        await userEvent.click(integrationToggle);

        expect(screen.getByText('Publish a version')).toBeInTheDocument();
    });

    it('shows the set-up title once the first call is made', async () => {
        mockStatus({...readyStatus, completed: true, completionType: 'COMPLETED', firstCallMade: true});

        renderPage();

        expect(screen.getByText("You're set up")).toBeInTheDocument();
        expect(screen.getByText('Next steps')).toBeInTheDocument();
        expect(screen.queryByText('Listening for the call')).not.toBeInTheDocument();
        expect(screen.queryByTestId('call-received')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: /Make your first call/}));

        expect(screen.getByTestId('call-received')).toHaveTextContent(
            'Call received' +
                'Your first call succeeded in Development. Setup is complete.' +
                'View external invocations'
        );
        expect(screen.queryByRole('button', {name: 'Dismiss'})).not.toBeInTheDocument();
    });

    it('keeps the getting started title for a dismissed tenant without a first call', () => {
        mockStatus({...baseStatus, completed: true, completionType: 'DISMISSED'});

        renderPage();

        expect(screen.getByText('Getting Started')).toBeInTheDocument();
        expect(screen.queryByText("You're set up")).not.toBeInTheDocument();
        expect(screen.queryByTestId('call-received')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Dismiss'})).not.toBeInTheDocument();
    });

    it('keeps the getting started title while listening in a completed tenant', () => {
        mockStatus({...readyStatus, completed: true, completionType: 'COMPLETED'});

        renderPage();

        expect(screen.getByText('Getting Started')).toBeInTheDocument();
        expect(screen.queryByText("You're set up")).not.toBeInTheDocument();
        expect(screen.getByText('Listening for the call')).toBeInTheDocument();
    });

    it('invalidates the completed query on unmount after completion, not before', () => {
        mockStatus({...readyStatus, completed: true, firstCallMade: true});

        const {unmount} = renderPage();

        expect(queryClient.invalidateQueries).not.toHaveBeenCalledWith({queryKey: ['embeddedGettingStartedCompleted']});

        unmount();

        expect(queryClient.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedGettingStartedCompleted']});
    });

    it('does not invalidate the completed query on unmount when not completed', () => {
        const {unmount} = renderPage();

        unmount();

        expect(queryClient.invalidateQueries).not.toHaveBeenCalledWith({queryKey: ['embeddedGettingStartedCompleted']});
    });

    it('dismiss asks for confirmation then calls the mutation and invalidates the completed query', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('button', {name: 'Dismiss'}));

        expect(hoisted.dismissMutateMock).not.toHaveBeenCalled();

        await userEvent.click(await screen.findByRole('button', {name: 'Dismiss Getting Started'}));

        expect(hoisted.dismissMutateMock).toHaveBeenCalled();

        const [mutationOptions] = hoisted.useDismissEmbeddedGettingStartedMutationMock.mock.lastCall ?? [];

        act(() => {
            mutationOptions.onSuccess();
        });

        await waitFor(() => {
            expect(queryClient.invalidateQueries).toHaveBeenCalledWith({
                queryKey: ['embeddedGettingStartedCompleted'],
            });
        });

        expect(queryClient.invalidateQueries).toHaveBeenCalledWith({queryKey: ['embeddedGettingStarted']});
    });

    it('keeps the connected user panel open when the test user step collapses after connecting', () => {
        mockStatus({...baseStatus, apiKeyCreated: true, testUserAdded: true, testUserId: '42'});

        const {rerender} = renderPage();

        act(() => {
            useConnectedUserSheetStore.setState({connectedUserId: 42, connectedUserSheetOpen: true});
        });

        mockStatus({...readyStatus, testUserId: '42'});

        rerender(
            <QueryClientProvider client={queryClient}>
                <TooltipProvider>
                    <MemoryRouter>
                        <EmbeddedGettingStarted />
                    </MemoryRouter>
                </TooltipProvider>
            </QueryClientProvider>
        );

        expect(useConnectedUserSheetStore.getState().connectedUserSheetOpen).toBe(true);
    });

    it('closes the connected user panel when the page goes away', () => {
        mockStatus({...baseStatus, testUserAdded: true, testUserId: '42'});

        const {unmount} = renderPage();

        act(() => {
            useConnectedUserSheetStore.setState({connectedUserId: 42, connectedUserSheetOpen: true});
        });

        unmount();

        expect(useConnectedUserSheetStore.getState().connectedUserSheetOpen).toBe(false);
    });

    it('offers support by email', () => {
        renderPage();

        expect(screen.getByText('Need help integrating ByteChef?')).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Contact support'})).toHaveAttribute(
            'href',
            'mailto:support@bytechef.io'
        );
    });
});
