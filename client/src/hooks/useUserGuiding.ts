import {UserI} from '@/shared/models/user.model';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useUserGuidingStore} from '@/shared/stores/useUserGuidingStore';
import {useCallback, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

const LAUNCHER_RETRY_INTERVAL = 500;
const LAUNCHER_RETRY_LIMIT = 40;

export function hideUserGuidingLauncher() {
    window.userGuiding?.hideResourceCenter?.();
    window.userGuiding?.hideChecklist();
}

export function showUserGuidingLauncher(): boolean {
    const userGuiding = window.userGuiding;
    const appState = userGuiding?.getAppState?.();

    const resourceCenterId = appState?.api?.resourceCenters?.find((resourceCenter) => resourceCenter.id != null)?.id;

    if (resourceCenterId != null && userGuiding?.launchResourceCenter) {
        userGuiding.launchResourceCenter(resourceCenterId);

        return true;
    }

    const checklistId = appState?.api?.assistants?.find((assistant) => assistant.id != null)?.id;

    if (checklistId != null && userGuiding) {
        userGuiding.launchChecklist(checklistId);

        return true;
    }

    return false;
}

export function hideLauncherOnceSdkIsReady() {
    let attempts = 0;

    const intervalId = window.setInterval(() => {
        attempts++;

        if (!useUserGuidingStore.getState().launcherHidden) {
            window.clearInterval(intervalId);

            return;
        }

        if (window.userGuiding?.hideResourceCenter) {
            hideUserGuidingLauncher();
        }

        if (attempts >= LAUNCHER_RETRY_LIMIT) {
            window.clearInterval(intervalId);

            hideUserGuidingLauncher();
        }
    }, LAUNCHER_RETRY_INTERVAL);
}

export function showLauncherOnceSdkIsReady() {
    let attempts = 0;

    const intervalId = window.setInterval(() => {
        attempts++;

        if (useUserGuidingStore.getState().launcherHidden) {
            window.clearInterval(intervalId);

            return;
        }

        if (showUserGuidingLauncher() || attempts >= LAUNCHER_RETRY_LIMIT) {
            window.clearInterval(intervalId);
        }
    }, LAUNCHER_RETRY_INTERVAL);
}

export function applyLauncherVisibilityOnceSdkIsReady() {
    if (useUserGuidingStore.getState().launcherHidden) {
        hideLauncherOnceSdkIsReady();
    } else {
        showLauncherOnceSdkIsReady();
    }
}

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

    applyLauncherVisibilityOnceSdkIsReady();
}

export interface UserGuidingI {
    identify(account: UserI): void;
    shutdown(): void;
}

export const useUserGuiding = (): UserGuidingI => {
    const {application, userGuiding} = useApplicationInfoStore(
        useShallow((state) => ({
            application: state.application,
            userGuiding: state.userGuiding,
        }))
    );

    const identifyRef = useRef(false);

    return {
        identify: (account: UserI) => {
            if (identifyRef.current) {
                return;
            }

            if (window.userGuiding && userGuiding.enabled) {
                identifyRef.current = true;

                window.userGuiding.identify(account.uuid, {
                    edition: application?.edition,
                    email: account.email,
                    name: `${account.firstName} ${account.lastName}`,
                });

                applyLauncherVisibilityOnceSdkIsReady();
            }
        },
        shutdown: () => {
            identifyRef.current = false;
        },
    };
};

export interface UserGuidingLauncherI {
    available: boolean;
    hideLauncher: () => void;
    launcherHidden: boolean;
    showLauncher: () => void;
}

export const useUserGuidingLauncher = (): UserGuidingLauncherI => {
    const userGuiding = useApplicationInfoStore((state) => state.userGuiding);

    const {launcherHidden, setLauncherHidden} = useUserGuidingStore(
        useShallow((state) => ({
            launcherHidden: state.launcherHidden,
            setLauncherHidden: state.setLauncherHidden,
        }))
    );

    const hideLauncher = useCallback(() => {
        hideUserGuidingLauncher();

        setLauncherHidden(true);
    }, [setLauncherHidden]);

    const showLauncher = useCallback(() => {
        showUserGuidingLauncher();

        setLauncherHidden(false);
    }, [setLauncherHidden]);

    return {
        available: userGuiding.enabled && !!userGuiding.containerId,
        hideLauncher,
        launcherHidden,
        showLauncher,
    };
};
