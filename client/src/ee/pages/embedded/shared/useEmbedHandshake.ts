import {AutomationHubThemeI} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
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

let unrestrictedParentOriginsWarned = false;

let verifiedParentOrigin: string | null = null;

export const getEmbedParentOrigin = (): string | null => verifiedParentOrigin;

const getBuildTimeParentOrigins = (): string[] => {
    const parentOriginsRaw = (import.meta.env.VITE_EMBEDDED_PARENT_ORIGINS as string | undefined) ?? '';

    return parentOriginsRaw
        .split(',')
        .map((origin) => origin.trim())
        .filter(Boolean);
};

const getReferrerOrigin = (): string | null => {
    if (!document.referrer) {
        return null;
    }

    try {
        return new URL(document.referrer).origin;
    } catch {
        return null;
    }
};

const warnUnrestrictedParentOriginsOnce = () => {
    if (unrestrictedParentOriginsWarned) {
        return;
    }

    unrestrictedParentOriginsWarned = true;

    console.warn(
        'ByteChef embedded page: no allowed parent origins are configured, so any website can embed this page and ' +
            'initialize it. Set BYTECHEF_EMBEDDED_ALLOWED_PARENT_ORIGINS on the ByteChef server to the origins of ' +
            'the applications that embed it.'
    );
};

export function useEmbedHandshake(onInit: (params: EmbedInitParamsI) => void, enabled = true): void {
    const onInitRef = useRef(onInit);

    const embedded = useApplicationInfoStore((state) => state.embedded);

    onInitRef.current = onInit;

    useEffect(() => {
        if (!enabled || !embedded) {
            return;
        }

        const allowedParentOrigins =
            embedded.allowedParentOrigins.length > 0 ? embedded.allowedParentOrigins : getBuildTimeParentOrigins();

        if (allowedParentOrigins.length === 0) {
            warnUnrestrictedParentOriginsOnce();
        }

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
                verifiedParentOrigin = event.origin;

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
            const referrerOrigin = getReferrerOrigin();

            let readyTargetOrigins: string[] = [];

            if (allowedParentOrigins.length > 0) {
                readyTargetOrigins = allowedParentOrigins;
            } else if (referrerOrigin) {
                readyTargetOrigins = [referrerOrigin];
            }

            for (const origin of readyTargetOrigins) {
                window.parent.postMessage({type: 'EMBED_READY'}, origin);
            }
        }

        return () => {
            window.removeEventListener('message', listener);
        };
    }, [embedded, enabled]);
}
