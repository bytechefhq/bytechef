import useEditionResolved from '@/shared/edition/useEditionResolved';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {Building2Icon, Loader2Icon} from 'lucide-react';
import {ReactNode} from 'react';

const EEVersion = ({children, hidden = false}: {children: ReactNode; hidden?: boolean}) => {
    const application = useApplicationInfoStore((state) => state.application);

    const editionResolved = useEditionResolved();

    if (application?.edition === 'EE') {
        return <>{children}</>;
    }

    if (hidden) {
        return <></>;
    }

    // While nobody knows which edition this is, "This is EE only feature." asserts something unestablished — and the
    // unresolved window is not always brief: `getApplicationInfo` assigns `application` only on HTTP 200 and never
    // retries, so one failed `/actuator/info` leaves the edition unknown for the whole session. Say what is true.
    if (!editionResolved) {
        return (
            <div className="flex size-full items-center justify-center">
                <div className="flex flex-col items-center space-y-2">
                    <Loader2Icon aria-hidden className="size-24 animate-spin text-gray-300" />

                    <p className="text-lg font-medium">Checking which features this installation includes…</p>

                    <p>Reload the page if this does not clear.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="flex size-full items-center justify-center">
            <div className="flex flex-col items-center space-y-2">
                <Building2Icon className="size-24 text-gray-300" />

                <p className="text-lg font-medium">This is EE only feature.</p>

                <p>Please contact support for more details.</p>
            </div>
        </div>
    );
};

export default EEVersion;
