import {describe, it, expect, vi, beforeEach, afterEach} from 'vitest';
import {renderHook, act} from '@testing-library/react';
import useConnectDialog from './index';
import {createRoot} from 'react-dom/client';

vi.mock('react-dom/client', () => ({
    createRoot: vi.fn(() => ({
        render: vi.fn(),
        unmount: vi.fn(),
    })),
}));

vi.mock('./useOAuth2', () => ({
    default: vi.fn(() => ({
        getAuth: vi.fn(),
    })),
}));

const defaultConnectDialogProps = {
    jwtToken: 'ey',
    integrationId: '1234',
    baseUrl: 'https://api.example.com',
};

describe('useConnectDialog', () => {
    it('should be defined', () => {
        expect(useConnectDialog).toBeDefined();
    });

    it('should return expected API with correct types', () => {
        const {result} = renderHook(() => useConnectDialog(defaultConnectDialogProps));

        expect(result.current).toHaveProperty('openDialog');
        expect(result.current).toHaveProperty('closeDialog');

        expect(typeof result.current.openDialog).toBe('function');
        expect(typeof result.current.closeDialog).toBe('function');
    });
});

describe('useConnectDialog - OAuth2 Detection', () => {
    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('sets the isOAuth2 state to true when OAuth2 is detected', async () => {
        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            json: vi.fn().mockResolvedValue({
                connectionConfig: {
                    authorizationType: 'OAUTH2_AUTHORIZATION_CODE',
                },
            }),
        });

        const renderMock = vi.fn();

        vi.mocked(createRoot).mockReturnValue({
            render: renderMock,
            unmount: vi.fn(),
        });

        const {result} = renderHook(() =>
            useConnectDialog({
                baseUrl: 'https://api.example.com',
                integrationId: '1234',
                jwtToken: 'ey',
            })
        );

        await act(async () => result.current.openDialog());

        const lastRenderCall = renderMock.mock.calls[renderMock.mock.calls.length - 1];

        const reactElement = lastRenderCall[0];
        const propsString = JSON.stringify(reactElement);

        expect(propsString.includes('"isOAuth2":true')).toBe(true);
    });
});

describe('useConnectDialog - API Configuration', () => {
    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('configures API requests properly', async () => {
        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            json: vi.fn().mockResolvedValue({}),
        });

        const customBaseUrl = 'https://custom-api.example.com';
        const customId = '1234';

        const {result: customResult} = renderHook(() =>
            useConnectDialog({
                baseUrl: customBaseUrl,
                integrationId: customId,
                jwtToken: 'ey',
            })
        );

        await act(async () => customResult.current.openDialog());

        expect(global.fetch).toHaveBeenCalledWith(
            `${customBaseUrl}/api/embedded/v1/integrations/${customId}`,
            expect.any(Object)
        );

        vi.clearAllMocks();

        const {result: defaultResult} = renderHook(() => useConnectDialog(defaultConnectDialogProps));

        await act(async () => defaultResult.current.openDialog());

        expect(global.fetch).toHaveBeenCalledWith(
            expect.any(String),
            expect.objectContaining({
                headers: expect.objectContaining({
                    Authorization: expect.stringContaining('Bearer ey'),
                    'x-environment': 'development',
                }),
            })
        );
    });
});

describe('useConnectDialog - Dialog State Management', () => {
    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('resets dialog state when closing', async () => {
        global.fetch = vi
            .fn()
            .mockResolvedValueOnce({
                ok: true,
                json: vi.fn().mockResolvedValue({id: 'first-call'}),
            })
            .mockResolvedValueOnce({
                ok: true,
                json: vi.fn().mockResolvedValue({id: 'second-call'}),
            });

        const {result} = renderHook(() => useConnectDialog(defaultConnectDialogProps));

        await act(async () => result.current.openDialog());

        act(() => result.current.closeDialog());

        await act(async () => result.current.openDialog());

        expect(global.fetch).toHaveBeenCalledTimes(2);
    });
});
