import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {ReactNode} from 'react';
import {Navigate} from 'react-router-dom';

const AiHubEnabled = ({children}: {children: ReactNode}) => {
    const aiHubEnabled = useApplicationInfoStore((state) => state.ai.hub.enabled);
    const application = useApplicationInfoStore((state) => state.application);

    if (application === null) {
        return null;
    }

    if (!aiHubEnabled) {
        return <Navigate replace to="/automation/projects" />;
    }

    return <>{children}</>;
};

export default AiHubEnabled;
