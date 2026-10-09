import {
    getEmbedCredentials,
    resetEmbedCredentials,
    setEmbedCredentials,
} from '@/ee/pages/embedded/shared/embedCredentials';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {QueryClient, QueryClientProvider, useQuery} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {getEmbedParentOrigin, useEmbedHandshake} from '../useEmbedHandshake';

const dispatchEmbedInit = (origin: string, source: Window) =>
    act(() => {
        window.dispatchEvent(
            new MessageEvent('message', {
                data: {params: {jwtToken: 'jwt-1'}, type: 'EMBED_INIT'},
                origin,
                source,
            })
        );
    });

let queryClient: QueryClient;

const wrapper = ({children}: {children: ReactNode}) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe('useEmbedHandshake', () => {
    beforeEach(() => {
        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        resetEmbedCredentials();
        sessionStorage.clear();

        applicationInfoStore.setState({embedded: {allowedParentOrigins: []}});

        vi.spyOn(console, 'warn').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
        resetEmbedCredentials();
        sessionStorage.clear();
        delete import.meta.env.VITE_EMBEDDED_PARENT_ORIGINS;

        applicationInfoStore.setState({embedded: null});
    });

    describe('with server-configured allowed parent origins', () => {
        beforeEach(() => {
            applicationInfoStore.setState({
                embedded: {allowedParentOrigins: ['https://a.example', 'https://b.example']},
            });
        });

        it('posts EMBED_READY only to the server-configured origins', () => {
            const postMessage = vi.fn();
            vi.spyOn(window, 'parent', 'get').mockReturnValue({postMessage} as unknown as Window);

            renderHook(() => useEmbedHandshake(vi.fn()), {wrapper});

            expect(postMessage).toHaveBeenCalledTimes(2);
            expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://a.example');
            expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://b.example');
            expect(postMessage).not.toHaveBeenCalledWith({type: 'EMBED_READY'}, '*');
        });

        it('ignores EMBED_INIT from an origin that the server does not allow', () => {
            const parent = {postMessage: vi.fn()} as unknown as Window;
            vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
            const onInit = vi.fn();

            renderHook(() => useEmbedHandshake(onInit), {wrapper});

            dispatchEmbedInit('https://evil.example', parent);

            expect(onInit).not.toHaveBeenCalled();
            expect(getEmbedCredentials().jwtToken).toBeNull();
        });

        it('accepts EMBED_INIT from an origin that the server allows', () => {
            const parent = {postMessage: vi.fn()} as unknown as Window;
            vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
            const onInit = vi.fn();

            renderHook(() => useEmbedHandshake(onInit), {wrapper});

            dispatchEmbedInit('https://b.example', parent);

            expect(onInit).toHaveBeenCalledWith(expect.objectContaining({jwtToken: 'jwt-1'}));
            expect(getEmbedCredentials().jwtToken).toBe('jwt-1');
        });

        it('uses the server-configured origins instead of the build-time origins', () => {
            import.meta.env.VITE_EMBEDDED_PARENT_ORIGINS = 'https://build.example';

            const parent = {postMessage: vi.fn()} as unknown as Window;
            vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
            const onInit = vi.fn();

            renderHook(() => useEmbedHandshake(onInit), {wrapper});

            dispatchEmbedInit('https://build.example', parent);

            expect(onInit).not.toHaveBeenCalled();
            expect(parent.postMessage).not.toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://build.example');
        });

        it('does not warn about unrestricted parent origins', () => {
            vi.spyOn(window, 'parent', 'get').mockReturnValue({postMessage: vi.fn()} as unknown as Window);

            renderHook(() => useEmbedHandshake(vi.fn()), {wrapper});

            expect(console.warn).not.toHaveBeenCalled();
        });
    });

    describe('before the server configuration has loaded', () => {
        beforeEach(() => {
            applicationInfoStore.setState({embedded: null});
        });

        it('neither posts EMBED_READY nor accepts EMBED_INIT', () => {
            const parent = {postMessage: vi.fn()} as unknown as Window;
            vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
            const onInit = vi.fn();

            renderHook(() => useEmbedHandshake(onInit), {wrapper});

            dispatchEmbedInit('https://evil.example', parent);

            expect(parent.postMessage).not.toHaveBeenCalled();
            expect(onInit).not.toHaveBeenCalled();
            expect(getEmbedCredentials().jwtToken).toBeNull();
        });

        it('starts the handshake with the server-configured origins once the configuration loads', () => {
            const parent = {postMessage: vi.fn()} as unknown as Window;
            vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
            const onInit = vi.fn();

            renderHook(() => useEmbedHandshake(onInit), {wrapper});

            act(() => {
                applicationInfoStore.setState({embedded: {allowedParentOrigins: ['https://a.example']}});
            });

            dispatchEmbedInit('https://evil.example', parent);
            dispatchEmbedInit('https://a.example', parent);

            expect(parent.postMessage).toHaveBeenCalledTimes(1);
            expect(parent.postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://a.example');
            expect(onInit).toHaveBeenCalledTimes(1);
        });
    });

    describe('without any configured allowed parent origins', () => {
        it('warns once that any origin can embed the page', async () => {
            vi.resetModules();

            const {applicationInfoStore: freshApplicationInfoStore} =
                await import('@/shared/stores/useApplicationInfoStore');
            const {useEmbedHandshake: freshUseEmbedHandshake} = await import('../useEmbedHandshake');
            const {QueryClient: FreshQueryClient, QueryClientProvider: FreshQueryClientProvider} =
                await import('@tanstack/react-query');

            const freshQueryClient = new FreshQueryClient();

            const freshWrapper = ({children}: {children: ReactNode}) => (
                <FreshQueryClientProvider client={freshQueryClient}>{children}</FreshQueryClientProvider>
            );

            freshApplicationInfoStore.setState({embedded: {allowedParentOrigins: []}});

            vi.spyOn(window, 'parent', 'get').mockReturnValue({postMessage: vi.fn()} as unknown as Window);

            renderHook(() => freshUseEmbedHandshake(vi.fn()), {wrapper: freshWrapper});
            renderHook(() => freshUseEmbedHandshake(vi.fn()), {wrapper: freshWrapper});

            expect(console.warn).toHaveBeenCalledTimes(1);
            expect(console.warn).toHaveBeenCalledWith(
                expect.stringContaining('BYTECHEF_EMBEDDED_ALLOWED_PARENT_ORIGINS')
            );
        });
    });

    it('posts EMBED_READY to the embedding page origin on mount', () => {
        const postMessage = vi.fn();
        vi.spyOn(window, 'parent', 'get').mockReturnValue({postMessage} as unknown as Window);
        vi.spyOn(document, 'referrer', 'get').mockReturnValue('https://host.example/settings?tab=1');

        renderHook(() => useEmbedHandshake(vi.fn()), {wrapper});

        expect(postMessage).toHaveBeenCalledTimes(1);
        expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://host.example');
        expect(postMessage).not.toHaveBeenCalledWith({type: 'EMBED_READY'}, '*');
    });

    it('does not post EMBED_READY to every origin when the embedding page origin is unknown', () => {
        const postMessage = vi.fn();
        vi.spyOn(window, 'parent', 'get').mockReturnValue({postMessage} as unknown as Window);
        vi.spyOn(document, 'referrer', 'get').mockReturnValue('');

        renderHook(() => useEmbedHandshake(vi.fn()), {wrapper});

        expect(postMessage).not.toHaveBeenCalled();
    });

    it('remembers the origin of the parent that sent EMBED_INIT', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);

        renderHook(() => useEmbedHandshake(vi.fn()), {wrapper});

        dispatchEmbedInit('https://parent.example', parent);

        expect(getEmbedParentOrigin()).toBe('https://parent.example');
    });

    it('does not post EMBED_READY when not running inside an iframe', () => {
        vi.spyOn(window, 'parent', 'get').mockReturnValue(window);
        const postMessageSpy = vi.spyOn(window, 'postMessage');

        renderHook(() => useEmbedHandshake(vi.fn()), {wrapper});

        expect(postMessageSpy).not.toHaveBeenCalled();
    });

    it('posts EMBED_READY to each allowed origin instead of "*" when an allow-list is configured', () => {
        import.meta.env.VITE_EMBEDDED_PARENT_ORIGINS = 'https://a.example, https://b.example';

        const postMessage = vi.fn();
        vi.spyOn(window, 'parent', 'get').mockReturnValue({postMessage} as unknown as Window);

        renderHook(() => useEmbedHandshake(vi.fn()), {wrapper});

        expect(postMessage).toHaveBeenCalledTimes(2);
        expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://a.example');
        expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://b.example');
        expect(postMessage).not.toHaveBeenCalledWith({type: 'EMBED_READY'}, '*');
    });

    it('stores the token and forwards params on EMBED_INIT from the parent', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        renderHook(() => useEmbedHandshake(onInit), {wrapper});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {
                        params: {environment: 'staging', jwtToken: 'jwt-1', tabs: {connections: false}},
                        type: 'EMBED_INIT',
                    },
                    origin: 'https://host.example',
                    source: parent,
                })
            );
        });

        expect(getEmbedCredentials().jwtToken).toBe('jwt-1');
        expect(getEmbedCredentials().environment).toBe('staging');
        expect(onInit).toHaveBeenCalledWith(
            expect.objectContaining({environment: 'staging', jwtToken: 'jwt-1', tabs: {connections: false}})
        );
    });

    it('defaults the stored environment to PRODUCTION when EMBED_INIT omits one', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);

        renderHook(() => useEmbedHandshake(vi.fn()), {wrapper});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {params: {jwtToken: 'jwt-1'}, type: 'EMBED_INIT'},
                    origin: 'https://host.example',
                    source: parent,
                })
            );
        });

        expect(getEmbedCredentials().environment).toBe('PRODUCTION');
    });

    it('forwards params and clears a previously stored token when EMBED_INIT carries no token', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        setEmbedCredentials({environment: 'PRODUCTION', jwtToken: 'previous-user-jwt'});

        renderHook(() => useEmbedHandshake(onInit), {wrapper});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {params: {includeComponents: ['slack']}, type: 'EMBED_INIT'},
                    origin: 'https://host.example',
                    source: parent,
                })
            );
        });

        expect(getEmbedCredentials().jwtToken).toBeNull();
        expect(getEmbedCredentials().environment).toBe('PRODUCTION');
        expect(onInit).toHaveBeenCalledWith(expect.objectContaining({includeComponents: ['slack']}));
    });

    it('ignores EMBED_INIT that does not come from the parent window', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        renderHook(() => useEmbedHandshake(onInit), {wrapper});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {params: {jwtToken: 'jwt-1'}, type: 'EMBED_INIT'},
                    origin: 'https://host.example',
                    source: window,
                })
            );
        });

        expect(onInit).not.toHaveBeenCalled();
        expect(getEmbedCredentials().jwtToken).toBeNull();
    });

    it('ignores EMBED_INIT from an origin that is not in the allow-list', () => {
        import.meta.env.VITE_EMBEDDED_PARENT_ORIGINS = 'https://allowed.example';

        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        renderHook(() => useEmbedHandshake(onInit), {wrapper});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {params: {jwtToken: 'jwt-1'}, type: 'EMBED_INIT'},
                    origin: 'https://evil.example',
                    source: parent,
                })
            );
        });

        expect(onInit).not.toHaveBeenCalled();
        expect(getEmbedCredentials().jwtToken).toBeNull();
    });

    it('accepts EMBED_INIT from an origin that is in the allow-list', () => {
        import.meta.env.VITE_EMBEDDED_PARENT_ORIGINS = 'https://allowed.example';

        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        renderHook(() => useEmbedHandshake(onInit), {wrapper});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {params: {jwtToken: 'jwt-1'}, type: 'EMBED_INIT'},
                    origin: 'https://allowed.example',
                    source: parent,
                })
            );
        });

        expect(onInit).toHaveBeenCalledWith(expect.objectContaining({jwtToken: 'jwt-1'}));
    });

    it('calls the latest onInit after a re-render, not the one captured at mount', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const firstOnInit = vi.fn();
        const secondOnInit = vi.fn();

        const {rerender} = renderHook(({onInit}) => useEmbedHandshake(onInit), {
            initialProps: {onInit: firstOnInit},
            wrapper,
        });

        rerender({onInit: secondOnInit});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {params: {jwtToken: 'jwt-1'}, type: 'EMBED_INIT'},
                    origin: 'https://host.example',
                    source: parent,
                })
            );
        });

        expect(secondOnInit).toHaveBeenCalledWith(expect.objectContaining({jwtToken: 'jwt-1'}));
        expect(firstOnInit).not.toHaveBeenCalled();
    });

    it('removes the message listener on unmount', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        const {unmount} = renderHook(() => useEmbedHandshake(onInit), {wrapper});

        unmount();

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {params: {jwtToken: 'jwt-1'}, type: 'EMBED_INIT'},
                    origin: 'https://host.example',
                    source: parent,
                })
            );
        });

        expect(onInit).not.toHaveBeenCalled();
    });

    it('replaces the stored token and environment when the parent sends EMBED_INIT again', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        renderHook(() => useEmbedHandshake(onInit), {wrapper});

        const sendInit = (params: object) =>
            act(() => {
                window.dispatchEvent(
                    new MessageEvent('message', {
                        data: {params, type: 'EMBED_INIT'},
                        origin: 'https://host.example',
                        source: parent,
                    })
                );
            });

        sendInit({environment: 'STAGING', jwtToken: 'jwt-1'});
        sendInit({environment: 'DEVELOPMENT', jwtToken: 'jwt-2'});

        expect(getEmbedCredentials().jwtToken).toBe('jwt-2');
        expect(getEmbedCredentials().environment).toBe('DEVELOPMENT');
        expect(onInit).toHaveBeenCalledTimes(2);
    });

    it('keeps the credentials out of sessionStorage that same-origin embedded frames of the host tab share', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);

        renderHook(() => useEmbedHandshake(vi.fn()), {wrapper});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {params: {environment: 'STAGING', jwtToken: 'jwt-1'}, type: 'EMBED_INIT'},
                    origin: 'https://host.example',
                    source: parent,
                })
            );
        });

        expect(sessionStorage.getItem('jwtToken')).toBeNull();
        expect(sessionStorage.getItem('environment')).toBeNull();
        expect(getEmbedCredentials()).toEqual({environment: 'STAGING', jwtToken: 'jwt-1'});
    });

    it('is not affected by credentials that another embedded frame writes to sessionStorage', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);

        renderHook(() => useEmbedHandshake(vi.fn()), {wrapper});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {params: {environment: 'STAGING', jwtToken: 'jwt-1'}, type: 'EMBED_INIT'},
                    origin: 'https://host.example',
                    source: parent,
                })
            );
        });

        sessionStorage.setItem('jwtToken', 'other-frame-jwt');
        sessionStorage.setItem('environment', 'DEVELOPMENT');

        expect(getEmbedCredentials()).toEqual({environment: 'STAGING', jwtToken: 'jwt-1'});
    });

    it('ignores a message from the parent that carries no data', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        renderHook(() => useEmbedHandshake(onInit), {wrapper});

        expect(() =>
            act(() => {
                window.dispatchEvent(
                    new MessageEvent('message', {data: null, origin: 'https://host.example', source: parent})
                );
            })
        ).not.toThrow();
        expect(onInit).not.toHaveBeenCalled();
    });

    describe('query cache on a repeated EMBED_INIT', () => {
        const sendInit = (parent: Window, params: object) =>
            act(() => {
                window.dispatchEvent(
                    new MessageEvent('message', {
                        data: {params, type: 'EMBED_INIT'},
                        origin: 'https://host.example',
                        source: parent,
                    })
                );
            });

        it('does not serve the previous connected user data after a new token is accepted', async () => {
            const parent = {postMessage: vi.fn()} as unknown as Window;
            vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);

            const fetchAutomations = vi.fn(async () => `automations of ${getEmbedCredentials().jwtToken}`);

            const {result} = renderHook(
                () => {
                    useEmbedHandshake(vi.fn());

                    return useQuery({queryFn: fetchAutomations, queryKey: ['automationHub', 'automations']});
                },
                {wrapper}
            );

            sendInit(parent, {environment: 'PRODUCTION', jwtToken: 'jwt-1'});

            await waitFor(() => expect(result.current.data).toBe('automations of jwt-1'));

            sendInit(parent, {environment: 'PRODUCTION', jwtToken: 'jwt-2'});

            expect(queryClient.getQueryData(['automationHub', 'automations'])).toBeUndefined();

            await waitFor(() => expect(result.current.data).toBe('automations of jwt-2'));
        });

        it('drops cached data of inactive queries when the environment changes', () => {
            const parent = {postMessage: vi.fn()} as unknown as Window;
            vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);

            renderHook(() => useEmbedHandshake(vi.fn()), {wrapper});

            sendInit(parent, {environment: 'STAGING', jwtToken: 'jwt-1'});

            queryClient.setQueryData(['integrationMarketplace', 'integrations'], ['staging integration']);

            sendInit(parent, {environment: 'PRODUCTION', jwtToken: 'jwt-1'});

            expect(queryClient.getQueryData(['integrationMarketplace', 'integrations'])).toBeUndefined();
        });

        it('keeps cached data when the same identity and environment are sent again', () => {
            const parent = {postMessage: vi.fn()} as unknown as Window;
            vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);

            renderHook(() => useEmbedHandshake(vi.fn()), {wrapper});

            sendInit(parent, {environment: 'STAGING', jwtToken: 'jwt-1'});

            queryClient.setQueryData(['integrationMarketplace', 'integrations'], ['staging integration']);

            sendInit(parent, {environment: 'STAGING', jwtToken: 'jwt-1'});

            expect(queryClient.getQueryData(['integrationMarketplace', 'integrations'])).toEqual([
                'staging integration',
            ]);
        });
    });
});
