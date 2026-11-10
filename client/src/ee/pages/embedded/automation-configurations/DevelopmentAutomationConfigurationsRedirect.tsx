import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ReactNode} from 'react';
import {Navigate, useSearchParams} from 'react-router-dom';

interface DevelopmentAutomationConfigurationsRedirectProps {
    children: ReactNode;
}

const DevelopmentAutomationConfigurationsRedirect = ({children}: DevelopmentAutomationConfigurationsRedirectProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [searchParams] = useSearchParams();

    if (currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT) {
        return <>{children}</>;
    }

    const projectId = searchParams.get('projectId');

    return (
        <Navigate
            replace
            to={projectId ? `/embedded/automations?projectId=${projectId}&tab=configurations` : '/embedded/automations'}
        />
    );
};

export default DevelopmentAutomationConfigurationsRedirect;
