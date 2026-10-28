import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ReactNode} from 'react';
import {Navigate, useLocation, useSearchParams} from 'react-router-dom';

interface DevelopmentProjectDeploymentsRedirectProps {
    children: ReactNode;
}

const DevelopmentProjectDeploymentsRedirect = ({children}: DevelopmentProjectDeploymentsRedirectProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const location = useLocation();
    const [searchParams] = useSearchParams();

    if (currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT) {
        return <>{children}</>;
    }

    const projectId = searchParams.get('projectId');

    return (
        <Navigate
            replace
            state={location.state}
            to={projectId ? `/automation/projects?projectId=${projectId}&tab=deployments` : '/automation/projects'}
        />
    );
};

export default DevelopmentProjectDeploymentsRedirect;
