import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {act, renderHook} from '@testing-library/react';
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

describe('useEmbedHandshake', () => {
    beforeEach(() => {
        sessionStorage.clear();

        applicationInfoStore.setState({embedded: {allowedParentOrigins: []}});

        vi.spyOn(console, 'warn').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
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

            renderHook(() => useEmbedHandshake(vi.fn()));

            expect(postMessage).toHaveBeenCalledTimes(2);
            expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://a.example');
            expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://b.example');
            expect(postMessage).not.toHaveBeenCalledWith({type: 'EMBED_READY'}, '*');
        });

        it('ignores EMBED_INIT from an origin that the server does not allow', () => {
            const parent = {postMessage: vi.fn()} as unknown as Window;
            vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
            const onInit = vi.fn();

            renderHook(() => useEmbedHandshake(onInit));

            dispatchEmbedInit('https://evil.example', parent);

            expect(onInit).not.toHaveBeenCalled();
            expect(sessionStorage.getItem('jwtToken')).toBeNull();
        });

        it('accepts EMBED_INIT from an origin that the server allows', () => {
            const parent = {postMessage: vi.fn()} as unknown as Window;
            vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
            const onInit = vi.fn();

            renderHook(() => useEmbedHandshake(onInit));

            dispatchEmbedInit('https://b.example', parent);

            expect(onInit).toHaveBeenCalledWith(expect.objectContaining({jwtToken: 'jwt-1'}));
            expect(sessionStorage.getItem('jwtToken')).toBe('jwt-1');
        });

        it('uses the server-configured origins instead of the build-time origins', () => {
            import.meta.env.VITE_EMBEDDED_PARENT_ORIGINS = 'https://build.example';

            const parent = {postMessage: vi.fn()} as unknown as Window;
            vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
            const onInit = vi.fn();

            renderHook(() => useEmbedHandshake(onInit));

            dispatchEmbedInit('https://build.example', parent);

            expect(onInit).not.toHaveBeenCalled();
            expect(parent.postMessage).not.toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://build.example');
        });

        it('does not warn about unrestricted parent origins', () => {
            vi.spyOn(window, 'parent', 'get').mockReturnValue({postMessage: vi.fn()} as unknown as Window);

            renderHook(() => useEmbedHandshake(vi.fn()));

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

            renderHook(() => useEmbedHandshake(onInit));

            dispatchEmbedInit('https://evil.example', parent);

            expect(parent.postMessage).not.toHaveBeenCalled();
            expect(onInit).not.toHaveBeenCalled();
            expect(sessionStorage.getItem('jwtToken')).toBeNull();
        });

        it('starts the handshake with the server-configured origins once the configuration loads', () => {
            const parent = {postMessage: vi.fn()} as unknown as Window;
            vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
            const onInit = vi.fn();

            renderHook(() => useEmbedHandshake(onInit));

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

            freshApplicationInfoStore.setState({embedded: {allowedParentOrigins: []}});

            vi.spyOn(window, 'parent', 'get').mockReturnValue({postMessage: vi.fn()} as unknown as Window);

            renderHook(() => freshUseEmbedHandshake(vi.fn()));
            renderHook(() => freshUseEmbedHandshake(vi.fn()));

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

        renderHook(() => useEmbedHandshake(vi.fn()));

        expect(postMessage).toHaveBeenCalledTimes(1);
        expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://host.example');
        expect(postMessage).not.toHaveBeenCalledWith({type: 'EMBED_READY'}, '*');
    });

    it('does not post EMBED_READY to every origin when the embedding page origin is unknown', () => {
        const postMessage = vi.fn();
        vi.spyOn(window, 'parent', 'get').mockReturnValue({postMessage} as unknown as Window);
        vi.spyOn(document, 'referrer', 'get').mockReturnValue('');

        renderHook(() => useEmbedHandshake(vi.fn()));

        expect(postMessage).not.toHaveBeenCalled();
    });

    it('remembers the origin of the parent that sent EMBED_INIT', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);

        renderHook(() => useEmbedHandshake(vi.fn()));

        dispatchEmbedInit('https://parent.example', parent);

        expect(getEmbedParentOrigin()).toBe('https://parent.example');
    });

    it('does not post EMBED_READY when not running inside an iframe', () => {
        vi.spyOn(window, 'parent', 'get').mockReturnValue(window);
        const postMessageSpy = vi.spyOn(window, 'postMessage');

        renderHook(() => useEmbedHandshake(vi.fn()));

        expect(postMessageSpy).not.toHaveBeenCalled();
    });

    it('posts EMBED_READY to each allowed origin instead of "*" when an allow-list is configured', () => {
        import.meta.env.VITE_EMBEDDED_PARENT_ORIGINS = 'https://a.example, https://b.example';

        const postMessage = vi.fn();
        vi.spyOn(window, 'parent', 'get').mockReturnValue({postMessage} as unknown as Window);

        renderHook(() => useEmbedHandshake(vi.fn()));

        expect(postMessage).toHaveBeenCalledTimes(2);
        expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://a.example');
        expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, 'https://b.example');
        expect(postMessage).not.toHaveBeenCalledWith({type: 'EMBED_READY'}, '*');
    });

    it('stores the token and forwards params on EMBED_INIT from the parent', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        renderHook(() => useEmbedHandshake(onInit));

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

        expect(sessionStorage.getItem('jwtToken')).toBe('jwt-1');
        expect(sessionStorage.getItem('environment')).toBe('staging');
        expect(onInit).toHaveBeenCalledWith(
            expect.objectContaining({environment: 'staging', jwtToken: 'jwt-1', tabs: {connections: false}})
        );
    });

    it('defaults the stored environment to PRODUCTION when EMBED_INIT omits one', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);

        renderHook(() => useEmbedHandshake(vi.fn()));

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {params: {jwtToken: 'jwt-1'}, type: 'EMBED_INIT'},
                    origin: 'https://host.example',
                    source: parent,
                })
            );
        });

        expect(sessionStorage.getItem('environment')).toBe('PRODUCTION');
    });

    it('forwards params and clears a previously stored token when EMBED_INIT carries no token', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        sessionStorage.setItem('jwtToken', 'previous-user-jwt');

        renderHook(() => useEmbedHandshake(onInit));

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {
                    data: {params: {includeComponents: ['slack']}, type: 'EMBED_INIT'},
                    origin: 'https://host.example',
                    source: parent,
                })
            );
        });

        expect(sessionStorage.getItem('jwtToken')).toBeNull();
        expect(sessionStorage.getItem('environment')).toBe('PRODUCTION');
        expect(onInit).toHaveBeenCalledWith(expect.objectContaining({includeComponents: ['slack']}));
    });

    it('ignores EMBED_INIT that does not come from the parent window', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        renderHook(() => useEmbedHandshake(onInit));

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
        expect(sessionStorage.getItem('jwtToken')).toBeNull();
    });

    it('ignores EMBED_INIT from an origin that is not in the allow-list', () => {
        import.meta.env.VITE_EMBEDDED_PARENT_ORIGINS = 'https://allowed.example';

        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        renderHook(() => useEmbedHandshake(onInit));

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
        expect(sessionStorage.getItem('jwtToken')).toBeNull();
    });

    it('accepts EMBED_INIT from an origin that is in the allow-list', () => {
        import.meta.env.VITE_EMBEDDED_PARENT_ORIGINS = 'https://allowed.example';

        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        renderHook(() => useEmbedHandshake(onInit));

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

        const {unmount} = renderHook(() => useEmbedHandshake(onInit));

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

        renderHook(() => useEmbedHandshake(onInit));

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

        expect(sessionStorage.getItem('jwtToken')).toBe('jwt-2');
        expect(sessionStorage.getItem('environment')).toBe('DEVELOPMENT');
        expect(onInit).toHaveBeenCalledTimes(2);
    });

    it('ignores a message from the parent that carries no data', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

        renderHook(() => useEmbedHandshake(onInit));

        expect(() =>
            act(() => {
                window.dispatchEvent(
                    new MessageEvent('message', {data: null, origin: 'https://host.example', source: parent})
                );
            })
        ).not.toThrow();
        expect(onInit).not.toHaveBeenCalled();
    });
});
