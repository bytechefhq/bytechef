import {act, render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationMarketplace from './IntegrationMarketplace';

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

const fireMessage = (data: unknown, source: unknown, origin = 'https://app.example') => {
    const messageEvent = new MessageEvent('message', {data, origin});

    Object.defineProperty(messageEvent, 'source', {value: source});

    act(() => {
        window.dispatchEvent(messageEvent);
    });
};

const getIframeWindow = (container: HTMLElement) => container.querySelector('iframe')!.contentWindow;

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

        fireMessage({type: 'EMBED_READY'}, iframe.contentWindow);

        expect(postMessage).toHaveBeenCalledWith(
            {params: {environment: 'STAGING', jwtToken: 'jwt-1', theme: {surfaceColor: '#fafafa'}}, type: 'EMBED_INIT'},
            'https://app.example'
        );
    });

    it('opens the connect dialog in the host page for the integration the iframe asked for', () => {
        const mapObjectFields = {Contacts: {}} as never;

        const {container} = render(
            <IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-1" mapObjectFields={mapObjectFields} />
        );

        expect(openDialogMock).not.toHaveBeenCalled();

        fireMessage({integrationId: '42', type: 'EMBED_OPEN_CONNECT_DIALOG'}, getIframeWindow(container));

        expect(connectDialogHookMock).toHaveBeenCalledWith(
            expect.objectContaining({integrationId: '42', mapObjectFields})
        );
        expect(openDialogMock).toHaveBeenCalledTimes(1);
    });

    it('tells the iframe to refresh its catalog when the connect dialog closes', () => {
        const {container} = render(<IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-1" />);
        const postMessage = vi.fn();

        Object.defineProperty(container.querySelector('iframe')!, 'contentWindow', {value: {postMessage}});

        fireMessage({integrationId: '42', type: 'EMBED_OPEN_CONNECT_DIALOG'}, getIframeWindow(container));

        const {onClose} = connectDialogHookMock.mock.calls[connectDialogHookMock.mock.calls.length - 1][0] as {
            onClose: () => void;
        };

        act(() => onClose());

        expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_INTEGRATIONS_CHANGED'}, 'https://app.example');
    });

    it('ignores a connect request from an origin that is not the hub', () => {
        const {container} = render(<IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-1" />);

        fireMessage(
            {integrationId: '42', type: 'EMBED_OPEN_CONNECT_DIALOG'},
            getIframeWindow(container),
            'https://evil.example'
        );

        expect(openDialogMock).not.toHaveBeenCalled();
    });

    it('ignores a connect request from another window on the hub origin', () => {
        render(<IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-1" />);

        fireMessage({integrationId: '42', type: 'EMBED_OPEN_CONNECT_DIALOG'}, window);

        expect(connectDialogHookMock).not.toHaveBeenCalled();
        expect(openDialogMock).not.toHaveBeenCalled();
    });

    it('re-sends EMBED_INIT when the token changes after the iframe is ready', () => {
        const {container, rerender} = render(<IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-1" />);
        const postMessage = vi.fn();

        Object.defineProperty(container.querySelector('iframe')!, 'contentWindow', {value: {postMessage}});

        fireMessage({type: 'EMBED_READY'}, getIframeWindow(container));

        postMessage.mockClear();

        rerender(<IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-2" />);

        expect(postMessage).toHaveBeenCalledTimes(1);
        expect(postMessage).toHaveBeenCalledWith(
            {params: {environment: 'PRODUCTION', jwtToken: 'jwt-2', theme: undefined}, type: 'EMBED_INIT'},
            'https://app.example'
        );
    });

    it('ignores a message without data', () => {
        const {container} = render(<IntegrationMarketplace baseUrl="https://app.example" jwtToken="jwt-1" />);

        expect(() => fireMessage(null, getIframeWindow(container))).not.toThrow();
    });
});
