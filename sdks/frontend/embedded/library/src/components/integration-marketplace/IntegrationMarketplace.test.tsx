import {act, render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationMarketplace from './IntegrationMarketplace';

// The connect dialog stands up its own OAuth/query graph; this suite only cares that the component
// opens it for the integration the iframe named, so the hook is replaced with a probe.
const {openDialogMock, connectDialogHookMock} = vi.hoisted(() => ({
    openDialogMock: vi.fn(),
    connectDialogHookMock: vi.fn(),
}));

vi.mock('../connect-dialog', () => ({
    default: (props: Record<string, unknown>) => {
        connectDialogHookMock(props);

        return {closeDialog: vi.fn(), openDialog: openDialogMock};
    },
}));

const fireFromIframe = (data: unknown, origin = 'https://app.example') => {
    act(() => {
        window.dispatchEvent(new MessageEvent('message', {data, origin}));
    });
};

describe('IntegrationMarketplace', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders the marketplace iframe and answers EMBED_READY with the init params', () => {
        const {container} = render(
            <IntegrationMarketplace
                baseUrl="https://app.example"
                className="h-96"
                environment="STAGING"
                jwtToken="jwt-1"
                theme={{surfaceColor: '#fafafa'}}
            />
        );
        const iframe = container.querySelector('iframe')!;

        expect(iframe.getAttribute('src')).toBe(
            'https://app.example/integration-marketplace.html#/embedded/marketplace'
        );
        expect(container.firstElementChild).toHaveClass('h-96');

        const postMessage = vi.fn();

        Object.defineProperty(iframe, 'contentWindow', {value: {postMessage}});

        fireFromIframe({type: 'EMBED_READY'});

        expect(postMessage).toHaveBeenCalledWith(
            {params: {environment: 'STAGING', jwtToken: 'jwt-1', theme: {surfaceColor: '#fafafa'}}, type: 'EMBED_INIT'},
            'https://app.example'
        );
    });

    it('opens the connect dialog in the host page for the integration the iframe asked for', () => {
        const mapObjectFields = {Contacts: {}} as never;

        render(
            <IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-1" mapObjectFields={mapObjectFields} />
        );

        expect(openDialogMock).not.toHaveBeenCalled();

        fireFromIframe({integrationId: '42', type: 'EMBED_OPEN_CONNECT_DIALOG'});

        // Rendered HERE rather than inside the iframe: `mapObjectFields` is made of functions, and
        // a function cannot cross a postMessage boundary.
        expect(connectDialogHookMock).toHaveBeenCalledWith(
            expect.objectContaining({integrationId: '42', mapObjectFields})
        );
        expect(openDialogMock).toHaveBeenCalledTimes(1);
    });

    // Connecting and disconnecting both happen in THIS page's dialog, which the catalog inside the
    // iframe cannot observe: without the message it keeps showing a disconnected integration as
    // "Connected" until the host page is reloaded.
    it('tells the iframe to refresh its catalog when the connect dialog closes', () => {
        const {container} = render(<IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-1" />);
        const postMessage = vi.fn();

        Object.defineProperty(container.querySelector('iframe')!, 'contentWindow', {value: {postMessage}});

        fireFromIframe({integrationId: '42', type: 'EMBED_OPEN_CONNECT_DIALOG'});

        const {onClose} = connectDialogHookMock.mock.calls.at(-1)![0] as {onClose: () => void};

        act(() => onClose());

        expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_INTEGRATIONS_CHANGED'}, 'https://app.example');
    });

    it('ignores a connect request from an origin that is not the hub', () => {
        render(<IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-1" />);

        fireFromIframe({integrationId: '42', type: 'EMBED_OPEN_CONNECT_DIALOG'}, 'https://evil.example');

        expect(openDialogMock).not.toHaveBeenCalled();
    });

    it('re-sends EMBED_INIT when the token changes after the iframe is ready', () => {
        const {container, rerender} = render(<IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-1" />);
        const postMessage = vi.fn();

        Object.defineProperty(container.querySelector('iframe')!, 'contentWindow', {value: {postMessage}});

        fireFromIframe({type: 'EMBED_READY'});

        postMessage.mockClear();

        rerender(<IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-2" />);

        expect(postMessage).toHaveBeenCalledTimes(1);
        expect(postMessage).toHaveBeenCalledWith(
            {params: {environment: 'PRODUCTION', jwtToken: 'jwt-2', theme: undefined}, type: 'EMBED_INIT'},
            'https://app.example'
        );
    });

    it('ignores a message without data', () => {
        render(<IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-1" />);

        expect(() => fireFromIframe(null)).not.toThrow();
    });
});
