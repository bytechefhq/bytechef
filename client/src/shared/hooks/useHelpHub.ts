import {UserI} from '@/shared/models/user.model';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {init} from 'commandbar';
import {useRef} from 'react';
import {useNavigate} from 'react-router-dom';

export interface HelpHubI {
    addRouter(): void;
    boot(account: UserI): void;
    init(): void;
    open(): void;
    shutdown(): void;
}

export const useHelpHub = (): HelpHubI => {
    const initRef = useRef(false);
    const bootRef = useRef(false);

    const {application, helpHub} = useApplicationInfoStore();

    const navigate = useNavigate();

    return {
        addRouter: () => {
            if (window.CommandBar) {
                const routerFunc = (newUrl: string) => navigate(newUrl);

                window.CommandBar.addRouter(routerFunc);
            }
        },
        boot: (account: UserI) => {
            if (bootRef.current) {
                return;
            }

            if (window.CommandBar) {
                bootRef.current = true;

                window.CommandBar.boot(account.uuid, {
                    edition: application?.edition,
                    email: account.email,
                    name: `${account.firstName} ${account.lastName}`,
                });
            }
        },
        init: () => {
            if (initRef.current) {
                return;
            }

            if (helpHub.enabled && helpHub.commandBar.orgId) {
                init(helpHub.commandBar.orgId);

                initRef.current = true;
            }
        },
        open: () => {
            if (window.CommandBar) {
                window.CommandBar.openHelpHub();
            }
        },
        shutdown: () => {
            if (window.CommandBar) {
                window.CommandBar.shutdown();
            }

            bootRef.current = false;
        },
    };
};
