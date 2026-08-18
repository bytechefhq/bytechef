import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useEmbedHandshake} from '../useEmbedHandshake';

describe('useEmbedHandshake', () => {
    beforeEach(() => {
        sessionStorage.clear();
    });

    afterEach(() => {
        vi.restoreAllMocks();
        sessionStorage.clear();
        delete import.meta.env.VITE_EMBEDDED_PARENT_ORIGINS;
    });

    it('posts EMBED_READY to the parent on mount', () => {
        const postMessage = vi.fn();
        vi.spyOn(window, 'parent', 'get').mockReturnValue({postMessage} as unknown as Window);

        renderHook(() => useEmbedHandshake(vi.fn()));

        expect(postMessage).toHaveBeenCalledWith({type: 'EMBED_READY'}, '*');
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

    it('forwards params but does not touch sessionStorage when EMBED_INIT carries no token', () => {
        const parent = {postMessage: vi.fn()} as unknown as Window;
        vi.spyOn(window, 'parent', 'get').mockReturnValue(parent);
        const onInit = vi.fn();

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
        expect(sessionStorage.getItem('environment')).toBeNull();
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
});
