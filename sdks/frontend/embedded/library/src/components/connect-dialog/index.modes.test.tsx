import {describe, it, expect, vi, beforeEach, afterEach} from 'vitest';
import {renderHook, act} from '@testing-library/react';
import useConnectDialog from './index';
import {createRoot} from 'react-dom/client';
import useOAuth2 from './useOAuth2';

vi.mock('react-dom/client', () => ({
    createRoot: vi.fn(() => ({render: vi.fn(), unmount: vi.fn()})),
}));

vi.mock('./useOAuth2', () => ({
    default: vi.fn(() => ({getAuth: vi.fn()})),
}));

const jsonResponse = (body: unknown) => ({
    ok: true,
    headers: {get: () => null},
    json: vi.fn().mockResolvedValue(body),
});

const slackIntegration = (instances: Array<{connectionId?: number; credentialStatus?: string; id: number}>) => ({
    connectionConfig: {authorizationType: 'OAUTH2_AUTHORIZATION_CODE', inputs: []},
    id: 12,
    integrationInstances: instances.map((instance) => ({
        credentialStatus: 'VALID',
        enabled: true,
        workflows: [],
        ...instance,
    })),
    name: 'Slack',
    workflows: [],
});

const lastRenderedProps = (renderMock: ReturnType<typeof vi.fn>) =>
    renderMock.mock.calls[renderMock.mock.calls.length - 1][0].props;

const latestOAuth2Options = () => {
    const calls = vi.mocked(useOAuth2).mock.calls;

    return calls[calls.length - 1][0];
};

const triggerOAuth2Code = (code: string) => {
    latestOAuth2Options().onCodeSuccess?.({code});
};

describe('useConnectDialog connection-only mode', () => {
    let renderMock: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        renderMock = vi.fn();
        vi.mocked(createRoot).mockReturnValue({render: renderMock, unmount: vi.fn()} as unknown as ReturnType<
            typeof createRoot
        >);
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('hands back the existing instance immediately when the integration already has one', async () => {
        global.fetch = vi.fn().mockResolvedValue(jsonResponse(slackIntegration([{id: 7, connectionId: 70}])));

        const onConnected = vi.fn();
        const onClose = vi.fn();

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                connectionOnly: true,
                integrationId: '12',
                jwtToken: 'ey',
                onClose,
                onConnected,
            })
        );

        await act(async () => result.current.openDialog());

        expect(onConnected).toHaveBeenCalledWith(7, 70);
        expect(onClose).toHaveBeenCalled();
        expect(global.fetch).toHaveBeenCalledTimes(1);
        expect(renderMock.mock.calls.every((call) => call[0].props.isOpen === false)).toBe(true);
    });

    it('shows the form for another account when asked for a new connection, and reports the one it creates', async () => {
        const fetchMock = vi
            .fn()
            .mockResolvedValueOnce(jsonResponse(slackIntegration([{connectionId: 70, id: 7}])))
            .mockResolvedValueOnce(jsonResponse(43))
            .mockResolvedValueOnce(
                jsonResponse(
                    slackIntegration([
                        {connectionId: 70, id: 7},
                        {connectionId: 430, id: 43},
                    ])
                )
            );

        global.fetch = fetchMock;

        const onConnected = vi.fn();

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                connectionOnly: true,
                integrationId: '12',
                jwtToken: 'ey',
                newConnection: true,
                onConnected,
            })
        );

        await act(async () => result.current.openDialog());

        expect(onConnected).not.toHaveBeenCalled();
        expect(lastRenderedProps(renderMock).isOpen).toBe(true);
        expect(lastRenderedProps(renderMock).title).toBeUndefined();

        await act(async () => triggerOAuth2Code('abc'));

        expect(fetchMock.mock.calls[1][0]).toBe('https://api.example.com/api/embedded/v1/integrations/12/instances');
        expect(onConnected).toHaveBeenCalledWith(43, 430);
    });

    it('creates an instance, reports its connection id and closes instead of showing workflows', async () => {
        const fetchMock = vi
            .fn()
            .mockResolvedValueOnce(jsonResponse(slackIntegration([])))
            .mockResolvedValueOnce(jsonResponse(42))
            .mockResolvedValueOnce(jsonResponse(slackIntegration([{id: 42, connectionId: 420}])));

        global.fetch = fetchMock;

        const onConnected = vi.fn();

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                connectionOnly: true,
                integrationId: '12',
                jwtToken: 'ey',
                onConnected,
            })
        );

        await act(async () => result.current.openDialog());

        expect(lastRenderedProps(renderMock).workflowsView).toBe(false);

        await act(async () => triggerOAuth2Code('abc'));

        expect(fetchMock.mock.calls[1][0]).toBe('https://api.example.com/api/embedded/v1/integrations/12/instances');
        expect(onConnected).toHaveBeenCalledWith(42, 420);
        expect(lastRenderedProps(renderMock).isOpen).toBe(false);
    });

    it('reports the instance it created, not the first one listed', async () => {
        const fetchMock = vi
            .fn()
            .mockResolvedValueOnce(jsonResponse(slackIntegration([])))
            .mockResolvedValueOnce(jsonResponse(42))
            .mockResolvedValueOnce(
                jsonResponse(
                    slackIntegration([
                        {connectionId: 80, id: 8},
                        {connectionId: 420, id: 42},
                    ])
                )
            );

        global.fetch = fetchMock;

        const onConnected = vi.fn();

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                connectionOnly: true,
                integrationId: '12',
                jwtToken: 'ey',
                onConnected,
            })
        );

        await act(async () => result.current.openDialog());
        await act(async () => triggerOAuth2Code('abc'));

        expect(onConnected).toHaveBeenCalledWith(42, 420);
    });

    it('closes without a save error when the host onConnected callback throws', async () => {
        global.fetch = vi
            .fn()
            .mockResolvedValueOnce(jsonResponse(slackIntegration([])))
            .mockResolvedValueOnce(jsonResponse(42))
            .mockResolvedValueOnce(jsonResponse(slackIntegration([{connectionId: 420, id: 42}])));

        const onClose = vi.fn();
        const onConnected = vi.fn(() => {
            throw new Error('host bug');
        });

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                connectionOnly: true,
                integrationId: '12',
                jwtToken: 'ey',
                onClose,
                onConnected,
            })
        );

        await act(async () => result.current.openDialog());
        await act(async () => triggerOAuth2Code('abc'));

        expect(onConnected).toHaveBeenCalledWith(42, 420);
        expect(onClose).toHaveBeenCalled();
        expect(lastRenderedProps(renderMock).saveError).toBeUndefined();
        expect(lastRenderedProps(renderMock).isOpen).toBe(false);
    });

    it('says the connection was saved when reloading the integration fails, without offering a retry save', async () => {
        const fetchMock = vi
            .fn()
            .mockResolvedValueOnce(jsonResponse(slackIntegration([])))
            .mockResolvedValueOnce(jsonResponse(42))
            .mockResolvedValueOnce({...jsonResponse({}), ok: false, status: 500});

        global.fetch = fetchMock;

        const onConnected = vi.fn();

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                connectionOnly: true,
                integrationId: '12',
                jwtToken: 'ey',
                onConnected,
            })
        );

        await act(async () => result.current.openDialog());
        await act(async () => triggerOAuth2Code('abc'));

        expect(onConnected).not.toHaveBeenCalled();
        expect(lastRenderedProps(renderMock).saveError).toMatch(/was saved, but it could not be loaded/);
    });

    it('reconnects an existing instance whose credentials are invalid and hands it back', async () => {
        const fetchMock = vi
            .fn()
            .mockResolvedValueOnce(
                jsonResponse(slackIntegration([{connectionId: 70, credentialStatus: 'INVALID', id: 7}]))
            )
            .mockResolvedValueOnce(jsonResponse({}));

        global.fetch = fetchMock;

        const onConnected = vi.fn();

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                connectionOnly: true,
                integrationId: '12',
                jwtToken: 'ey',
                onConnected,
            })
        );

        await act(async () => result.current.openDialog());

        expect(onConnected).not.toHaveBeenCalled();
        expect(lastRenderedProps(renderMock).isOpen).toBe(true);
        expect(lastRenderedProps(renderMock).title).toBe('Reconnect Slack');

        await act(async () => triggerOAuth2Code('fresh'));

        expect(fetchMock.mock.calls[1][0]).toBe('https://api.example.com/api/embedded/v1/connections/70/reauthorize');
        expect(onConnected).toHaveBeenCalledWith(7, 70);
    });

    it('shows the form when the existing instance has no connection', async () => {
        global.fetch = vi.fn().mockResolvedValue(jsonResponse(slackIntegration([{id: 7}])));

        const onConnected = vi.fn();

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                connectionOnly: true,
                integrationId: '12',
                jwtToken: 'ey',
                onConnected,
            })
        );

        await act(async () => result.current.openDialog());

        expect(onConnected).not.toHaveBeenCalled();
        expect(lastRenderedProps(renderMock).isOpen).toBe(true);
        expect(lastRenderedProps(renderMock).title).toBeUndefined();
    });

    it('shows an error in the dialog when OAuth2 authorization fails', async () => {
        global.fetch = vi.fn().mockResolvedValue(jsonResponse(slackIntegration([])));

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                connectionOnly: true,
                integrationId: '12',
                jwtToken: 'ey',
            })
        );

        await act(async () => result.current.openDialog());
        await act(async () => latestOAuth2Options().onError?.('access_denied'));

        expect(lastRenderedProps(renderMock).saveError).toBe('Could not authorize the connection. Please try again.');
    });
});

describe('useConnectDialog reauthorize mode', () => {
    let renderMock: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        renderMock = vi.fn();
        vi.mocked(createRoot).mockReturnValue({render: renderMock, unmount: vi.fn()} as unknown as ReturnType<
            typeof createRoot
        >);
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('shows the connection form titled Reconnect and posts to the reauthorize endpoint', async () => {
        const fetchMock = vi
            .fn()
            .mockResolvedValueOnce(jsonResponse(slackIntegration([{id: 7, connectionId: 70}])))
            .mockResolvedValueOnce(jsonResponse({}));

        global.fetch = fetchMock;

        const onReauthorized = vi.fn();

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                integrationId: '12',
                jwtToken: 'ey',
                onReauthorized,
                reauthorizeConnectionId: 70,
            })
        );

        await act(async () => result.current.openDialog());

        const props = lastRenderedProps(renderMock);

        expect(props.workflowsView).toBe(false);
        expect(props.title).toBe('Reconnect Slack');

        await act(async () => triggerOAuth2Code('fresh'));

        expect(fetchMock.mock.calls[1][0]).toBe('https://api.example.com/api/embedded/v1/connections/70/reauthorize');
        expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({parameters: {code: 'fresh'}});
        expect(fetchMock).toHaveBeenCalledTimes(2);
        expect(onReauthorized).toHaveBeenCalledWith(70);
        expect(lastRenderedProps(renderMock).isOpen).toBe(false);
    });

    it('shows a save error in the dialog when reauthorizing fails, and clears it on the next attempt', async () => {
        const fetchMock = vi
            .fn()
            .mockResolvedValueOnce(jsonResponse(slackIntegration([{id: 7, connectionId: 70}])))
            .mockResolvedValueOnce({...jsonResponse({}), ok: false, status: 500})
            .mockResolvedValueOnce(jsonResponse({}));

        global.fetch = fetchMock;

        const onReauthorized = vi.fn();

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                integrationId: '12',
                jwtToken: 'ey',
                onReauthorized,
                reauthorizeConnectionId: 70,
            })
        );

        await act(async () => result.current.openDialog());

        expect(lastRenderedProps(renderMock).saveError).toBeUndefined();

        await act(async () => triggerOAuth2Code('fresh'));

        expect(onReauthorized).not.toHaveBeenCalled();
        expect(lastRenderedProps(renderMock).isOpen).toBe(true);
        expect(lastRenderedProps(renderMock).saveError).toBe('Could not save the connection. Please try again.');

        await act(async () => triggerOAuth2Code('fresh-again'));

        expect(onReauthorized).toHaveBeenCalledWith(70);
        expect(lastRenderedProps(renderMock).saveError).toBeUndefined();
    });

    it('closes without a save error when the host onReauthorized callback throws', async () => {
        global.fetch = vi
            .fn()
            .mockResolvedValueOnce(jsonResponse(slackIntegration([{connectionId: 70, id: 7}])))
            .mockResolvedValueOnce(jsonResponse({}));

        const onReauthorized = vi.fn(() => {
            throw new Error('host bug');
        });

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                integrationId: '12',
                jwtToken: 'ey',
                onReauthorized,
                reauthorizeConnectionId: 70,
            })
        );

        await act(async () => result.current.openDialog());
        await act(async () => triggerOAuth2Code('fresh'));

        expect(onReauthorized).toHaveBeenCalledWith(70);
        expect(lastRenderedProps(renderMock).saveError).toBeUndefined();
        expect(lastRenderedProps(renderMock).isOpen).toBe(false);
    });

    it('reauthorizes through the non-OAuth2 form too', async () => {
        const fetchMock = vi
            .fn()
            .mockResolvedValueOnce(
                jsonResponse({
                    ...slackIntegration([{connectionId: 70, id: 7}]),
                    connectionConfig: {authorizationType: 'API_KEY', inputs: [{label: 'Key', name: 'key'}]},
                })
            )
            .mockResolvedValueOnce(jsonResponse({}));

        global.fetch = fetchMock;

        const onReauthorized = vi.fn();

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                integrationId: '12',
                jwtToken: 'ey',
                onReauthorized,
                reauthorizeConnectionId: 70,
            })
        );

        await act(async () => result.current.openDialog());

        const props = lastRenderedProps(renderMock);

        props.registerFormSubmit((callback: (data: Record<string, string>) => void) => () => callback({key: 'secret'}));

        await act(async () => props.handleClick({target: {name: 'connectButton'}}));

        expect(fetchMock.mock.calls[1][0]).toBe('https://api.example.com/api/embedded/v1/connections/70/reauthorize');
        expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({parameters: {key: 'secret'}});
        expect(onReauthorized).toHaveBeenCalledWith(70);
    });
});
