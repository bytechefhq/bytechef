import {UserI} from '@/shared/models/user.model';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useRef} from 'react';
// TODO: restore once /actuator/info exposes userGuiding
// import {useShallow} from 'zustand/react/shallow';

export function initUserGuiding(containerId: string) {
    window.userGuidingLayer = window.userGuidingLayer || [];

    if (window.userGuiding) {
        return;
    }

    const userGuiding: Window['userGuiding'] = {
        c:
            (name: string) =>
            (...arguments_: unknown[]) => {
                userGuiding!.q.push([name, arguments_]);
            },
        finishPreview: () => {},
        hideChecklist: () => {},
        identify: () => {},
        launchChecklist: () => {},
        previewGuide: () => {},
        q: [],
        track: () => {},
    };

    const methods = ['previewGuide', 'finishPreview', 'track', 'identify', 'hideChecklist', 'launchChecklist'] as const;

    for (const method of methods) {
        userGuiding[method] = userGuiding.c(method) as (typeof userGuiding)[typeof method];
    }

    window.userGuiding = userGuiding;

    const script = document.createElement('script');

    script.async = true;
    script.src = `https://static.userguiding.com/media/user-guiding-${containerId}-embedded.js`;

    document.head.appendChild(script);
}

export interface UserGuidingI {
    identify(account: UserI): void;
    shutdown(): void;
}

export const useUserGuiding = (): UserGuidingI => {
    // TODO: restore userGuiding from store once /actuator/info exposes it
    // const {application, userGuiding} = useApplicationInfoStore(
    //     useShallow((state) => ({
    //         application: state.application,
    //         userGuiding: state.userGuiding,
    //     }))
    // );
    const application = useApplicationInfoStore((state) => state.application);

    const identifyRef = useRef(false);

    return {
        identify: (account: UserI) => {
            if (identifyRef.current) {
                return;
            }

            // TODO: restore backend-driven gate once /actuator/info exposes userGuiding
            // if (window.userGuiding && userGuiding.enabled) {
            if (window.userGuiding) {
                identifyRef.current = true;

                window.userGuiding.identify(account.uuid, {
                    edition: application?.edition,
                    email: account.email,
                    name: `${account.firstName} ${account.lastName}`,
                });
            }
        },
        shutdown: () => {
            identifyRef.current = false;
        },
    };
};
