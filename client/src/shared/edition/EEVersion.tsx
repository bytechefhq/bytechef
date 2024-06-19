import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {Building2Icon} from 'lucide-react';
import {ReactNode} from 'react';

const EEVersion = ({children}: {children: ReactNode}) => {
    const {application} = useApplicationInfoStore();

    if (application?.edition === 'ee') {
        return <>{children}</>;
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
