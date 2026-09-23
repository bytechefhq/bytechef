import IntegrationMarketplaceView from '@/ee/pages/embedded/integration-marketplace/IntegrationMarketplaceView';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

// vi.mock factories hoist above module-scope consts, so the refs they close over come from
// vi.hoisted — see CLAUDE.md's Vitest mock factory hoisting note.
const {getFrontendIntegrationsMock} = vi.hoisted(() => ({getFrontendIntegrationsMock: vi.fn()}));

vi.mock('@/ee/shared/middleware/embedded/public', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/ee/shared/middleware/embedded/public')>()),
    IntegrationApi: class {
        getFrontendIntegrations = getFrontendIntegrationsMock;
    },
}));

vi.mock('react-inlinesvg', () => ({default: ({src}: {src: string}) => <span data-testid="icon">{src}</span>}));

const CONNECTED_INTEGRATION = {
    componentName: 'slack',
    description: 'Team messaging',
    id: 7,
    integrationInstances: [{id: 3}],
    name: 'Slack',
};

const DISCONNECTED_INTEGRATION = {
    componentName: 'gmail',
    description: 'Email',
    id: 8,
    integrationInstances: [],
    name: 'Gmail',
};

const renderView = () =>
    render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <IntegrationMarketplaceView />
        </QueryClientProvider>
    );

describe('IntegrationMarketplaceView', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        getFrontendIntegrationsMock.mockResolvedValue([CONNECTED_INTEGRATION, DISCONNECTED_INTEGRATION]);
    });

    it('says which integrations the connected user already has', async () => {
        renderView();

        expect(await screen.findByText('Slack')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Connected'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Connect'})).toBeInTheDocument();
    });

    it('asks the host page to open its own connect dialog, since a function cannot cross postMessage', async () => {
        const user = userEvent.setup();
        const postMessageSpy = vi.spyOn(window.parent, 'postMessage');

        renderView();

        await user.click(await screen.findByRole('button', {name: 'Connect'}));

        expect(postMessageSpy).toHaveBeenCalledWith({integrationId: '8', type: 'EMBED_OPEN_CONNECT_DIALOG'}, '*');
    });

    // The connect dialog belongs to the HOST page, so connecting or disconnecting happens where this
    // catalog cannot see it — without the refresh message a disconnected integration keeps reading
    // "Connected" until the host page is reloaded.
    it('refetches when the host reports the connect dialog changed something', async () => {
        renderView();

        expect(await screen.findByRole('button', {name: 'Connected'})).toBeInTheDocument();

        getFrontendIntegrationsMock.mockResolvedValue([
            {...CONNECTED_INTEGRATION, integrationInstances: []},
            DISCONNECTED_INTEGRATION,
        ]);

        window.dispatchEvent(
            new MessageEvent('message', {data: {type: 'EMBED_INTEGRATIONS_CHANGED'}, source: window.parent})
        );

        await waitFor(() => expect(screen.getAllByRole('button', {name: 'Connect'})).toHaveLength(2));
    });

    it('ignores a refresh message that did not come from the host page', async () => {
        renderView();

        expect(await screen.findByRole('button', {name: 'Connected'})).toBeInTheDocument();
        expect(getFrontendIntegrationsMock).toHaveBeenCalledTimes(1);

        window.dispatchEvent(new MessageEvent('message', {data: {type: 'EMBED_INTEGRATIONS_CHANGED'}, source: null}));

        await new Promise((resolve) => setTimeout(resolve, 20));

        expect(getFrontendIntegrationsMock).toHaveBeenCalledTimes(1);
    });
});
