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
    environment: 'DEVELOPMENT',
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
            json: vi.fn().mockResolvedValue({
                name: 'Test Integration',
                workflows: [],
                integrationInstances: [],
            }),
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
                    'X-Environment': 'DEVELOPMENT',
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

    it('sets workflowsView to true when integrationInstanceId is provided', async () => {
        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            json: vi.fn().mockResolvedValue({
                name: 'Test Integration',
                workflows: [],
                integrationInstances: [],
            }),
        });

        const renderMock = vi.fn();

        vi.mocked(createRoot).mockReturnValue({
            render: renderMock,
            unmount: vi.fn(),
        });

        const {result} = renderHook(() =>
            useConnectDialog({
                ...defaultConnectDialogProps,
                integrationInstanceId: '123',
            })
        );

        await act(async () => result.current.openDialog());

        const renderedProps = renderMock.mock.calls[renderMock.mock.calls.length - 1][0].props;

        expect(renderedProps.workflowsView).toBe(true);
    });
});

describe('useConnectDialog - Form Validation', () => {
    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('validates form inputs correctly', async () => {
        const originalCreateElement = document.createElement.bind(document);

        global.fetch = vi.fn().mockResolvedValue({
            ok: true,
            json: vi.fn().mockResolvedValue({
                connectionConfig: {
                    authorizationType: 'API_KEY',
                    inputs: [
                        {name: 'apiKey', label: 'API Key', required: true},
                        {name: 'domain', label: 'Domain', required: false},
                    ],
                },
            }),
        });

        const inputElements = {
            apiKey: {value: ''},
            domain: {value: 'example.com'},
        };

        document.createElement = vi.fn().mockImplementation((tag) => {
            if (tag === 'input') {
                return inputElements.apiKey;
            }

            return originalCreateElement(tag);
        });

        try {
            const renderMock = vi.fn();

            vi.mocked(createRoot).mockReturnValue({
                render: renderMock,
                unmount: vi.fn(),
            });

            const {result} = renderHook(() => useConnectDialog(defaultConnectDialogProps));

            await act(async () => result.current.openDialog());

            const lastRenderCall = renderMock.mock.calls[renderMock.mock.calls.length - 1];
            const props = lastRenderCall[0].props;

            expect(props.form.formState.errors).toBeDefined();
        } finally {
            document.createElement = originalCreateElement;
        }
    });
});

describe('useConnectDialog - Navigation', () => {
    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('makes correct API calls when toggling workflows', async () => {
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            headers: {
                get: (name: string) => (name === 'content-length' ? '2' : null),
            },
            json: vi.fn().mockResolvedValue({
                workflows: [
                    {
                        label: 'Test Workflow',
                        workflowUuid: 'workflow-123',
                    },
                ],
                integrationInstances: [
                    {
                        id: 123,
                        enabled: true,
                        credentialStatus: 'VALID',
                        workflows: [],
                    },
                ],
            }),
        });

        global.fetch = fetchMock as unknown as typeof global.fetch;

        const renderMock = vi.fn();

        vi.mocked(createRoot).mockReturnValue({
            render: renderMock,
            unmount: vi.fn(),
        });

        const {result} = renderHook(() =>
            useConnectDialog({
                ...defaultConnectDialogProps,
                integrationInstanceId: '123',
            })
        );

        await act(async () => result.current.openDialog());

        fetchMock.mockClear();

        const props = renderMock.mock.calls[renderMock.mock.calls.length - 1][0].props;

        await act(async () => {
            props.handleWorkflowToggle('workflow-123', true);
        });

        expect(fetchMock).toHaveBeenCalledWith(
            expect.stringContaining('/workflows/workflow-123/enable'),
            expect.objectContaining({method: 'POST'})
        );

        fetchMock.mockClear();

        await act(async () => {
            props.handleWorkflowToggle('workflow-123', false);
        });

        expect(fetchMock).toHaveBeenCalledWith(
            expect.stringContaining('/workflows/workflow-123/enable'),
            expect.objectContaining({method: 'DELETE'})
        );
    });
});
