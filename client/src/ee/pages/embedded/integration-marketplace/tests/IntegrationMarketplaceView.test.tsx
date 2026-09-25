import IntegrationMarketplaceView from '@/ee/pages/embedded/integration-marketplace/IntegrationMarketplaceView';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {getEmbedParentOriginMock, getFrontendIntegrationsMock} = vi.hoisted(() => ({
    getEmbedParentOriginMock: vi.fn(),
    getFrontendIntegrationsMock: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/shared/useEmbedHandshake', () => ({getEmbedParentOrigin: getEmbedParentOriginMock}));

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

        getEmbedParentOriginMock.mockReturnValue('https://host.example');
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

        expect(postMessageSpy).toHaveBeenCalledWith(
            {integrationId: '8', type: 'EMBED_OPEN_CONNECT_DIALOG'},
            'https://host.example'
        );
    });

    it('does not send a connect request before the host page has initialized the handshake', async () => {
        const user = userEvent.setup();
        const postMessageSpy = vi.spyOn(window.parent, 'postMessage');

        getEmbedParentOriginMock.mockReturnValue(null);

        renderView();

        await user.click(await screen.findByRole('button', {name: 'Connect'}));

        expect(postMessageSpy).not.toHaveBeenCalled();
    });

    it('refetches when the host reports the connect dialog changed something', async () => {
        renderView();

        expect(await screen.findByRole('button', {name: 'Connected'})).toBeInTheDocument();

        getFrontendIntegrationsMock.mockResolvedValue([
            {...CONNECTED_INTEGRATION, integrationInstances: []},
            DISCONNECTED_INTEGRATION,
        ]);

        window.dispatchEvent(
            new MessageEvent('message', {
                data: {type: 'EMBED_INTEGRATIONS_CHANGED'},
                origin: 'https://host.example',
                source: window.parent,
            })
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

    it('ignores a refresh message from an origin other than the host page', async () => {
        renderView();

        expect(await screen.findByRole('button', {name: 'Connected'})).toBeInTheDocument();

        window.dispatchEvent(
            new MessageEvent('message', {
                data: {type: 'EMBED_INTEGRATIONS_CHANGED'},
                origin: 'https://evil.example',
                source: window.parent,
            })
        );

        await new Promise((resolve) => setTimeout(resolve, 20));

        expect(getFrontendIntegrationsMock).toHaveBeenCalledTimes(1);
    });
});
