import {AutomationHubThemeI} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {useEffect, useRef} from 'react';

export interface EmbedInitParamsI {
    connectionDialogAllowed?: boolean;
    defaultLayout?: 'grid' | 'list';
    editWorkflowAllowed?: boolean;
    environment?: string;
    includeComponents?: string[];
    jwtToken?: string;
    layoutSwitcherAllowed?: boolean;
    tabs?: {automations?: boolean; connections?: boolean; newWorkflow?: boolean};
    theme?: AutomationHubThemeI;
}

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

            if (event.data?.type === 'EMBED_INIT') {
                const params = (event.data.params ?? {}) as EmbedInitParamsI;

                const environment = params.environment || 'PRODUCTION';
                const jwtToken = params.jwtToken;

                if (jwtToken) {
                    sessionStorage.setItem('jwtToken', jwtToken);
                } else {
                    sessionStorage.removeItem('jwtToken');
                }

                sessionStorage.setItem('environment', environment);

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
