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

const slackIntegration = (instances: Array<{id: number; connectionId?: number}>) => ({
    connectionConfig: {authorizationType: 'OAUTH2_AUTHORIZATION_CODE', inputs: []},
    id: 12,
    integrationInstances: instances.map((instance) => ({
        ...instance,
        enabled: true,
        credentialStatus: 'VALID',
        workflows: [],
    })),
    name: 'Slack',
    workflows: [],
});

const lastRenderedProps = (renderMock: ReturnType<typeof vi.fn>) =>
    renderMock.mock.calls[renderMock.mock.calls.length - 1][0].props;

const triggerOAuth2Code = (code: string) => {
    const calls = vi.mocked(useOAuth2).mock.calls;
    const options = calls[calls.length - 1][0];

    options.onCodeSuccess?.({code});
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
});
