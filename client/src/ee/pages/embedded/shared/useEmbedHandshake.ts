import {useEffect, useRef} from 'react';

export interface EmbedInitParamsI {
    connectionDialogAllowed?: boolean;
    environment?: string;
    includeComponents?: string[];
    jwtToken?: string;
    sharedConnectionIds?: number[];
    tabs?: {automations?: boolean; connections?: boolean; newWorkflow?: boolean};
    theme?: {borderRadius?: string; fontFamily?: string; mode?: 'dark' | 'light'; primaryColor?: string};
}

/**
 * Runs the embed handshake shared by every embedded surface (workflow builder, automation hub):
 * broadcasts `EMBED_READY` to the parent frame on mount, then listens for the parent's
 * `EMBED_INIT` reply. Only messages that come from `window.parent` and, when configured, from an
 * allowed origin are honored. The JWT token and environment carried by `EMBED_INIT` are written to
 * `sessionStorage` (read by `useFetchInterceptor` for every subsequent API call) before `onInit` is
 * invoked with the raw params so the caller can apply surface-specific state.
 *
 * The listener is registered exactly once per mount (deps are just `[enabled]`, and `enabled` is
 * a stable primitive across a given mount's lifetime for every caller today), but `onInit` is read
 * through a ref that is reassigned on every render, so a caller whose callback closes over
 * changing values (e.g. a React context) always invokes its latest version rather than the one
 * captured at mount time.
 *
 * `enabled` (default `true`) lets a caller that already has the handshake's result — the workflow
 * builder rendering inside the Automation Hub, which forwards the hub's own `EMBED_INIT` params
 * via `HubBuilderContext` instead — skip broadcasting a second, redundant `EMBED_READY`.
 */
export function useEmbedHandshake(onInit: (params: EmbedInitParamsI) => void, enabled = true): void {
    const onInitRef = useRef(onInit);

    onInitRef.current = onInit;

    useEffect(() => {
        if (!enabled) {
            return;
        }

        const parentOriginsRaw = (import.meta.env.VITE_EMBEDDED_PARENT_ORIGINS as string | undefined) ?? '';
        const allowedParentOrigins = parentOriginsRaw
            .split(',')
            .map((origin) => origin.trim())
            .filter(Boolean);

        const isAllowedOrigin = (origin: string) =>
            allowedParentOrigins.length === 0 || allowedParentOrigins.includes(origin);

        const listener = (event: MessageEvent) => {
            if (event.source !== window.parent || event.source === window) {
                return;
            }

            if (!isAllowedOrigin(event.origin)) {
                return;
            }

            if (event.data.type === 'EMBED_INIT') {
                const params = event.data.params as EmbedInitParamsI;

                const environment = params.environment || 'PRODUCTION';
                const jwtToken = params.jwtToken;

                if (jwtToken) {
                    sessionStorage.setItem('jwtToken', jwtToken);
                    sessionStorage.setItem('environment', environment);
                }

                onInitRef.current(params);
            }
        };

        window.addEventListener('message', listener);

        if (window.parent !== window) {
            if (allowedParentOrigins.length === 0) {
                window.parent.postMessage({type: 'EMBED_READY'}, '*');
            } else {
                for (const origin of allowedParentOrigins) {
                    window.parent.postMessage({type: 'EMBED_READY'}, origin);
                }
            }
        }

        return () => {
            window.removeEventListener('message', listener);
        };
    }, [enabled]);
}
