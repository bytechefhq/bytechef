import {UserI} from '@/shared/models/user.model';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useRef} from 'react';
import {useNavigate} from 'react-router-dom';

export interface HelpHubI {
    addRouter(): void;
    boot(account: UserI): void;
    open(): void;
    shutdown(): void;
}

export const useHelpHub = (): HelpHubI => {
    const application = useApplicationInfoStore((state) => state.application);

    const navigate = useNavigate();

    const bootRef = useRef(false);

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
