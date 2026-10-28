import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ReactNode} from 'react';
import {Navigate, useLocation, useSearchParams} from 'react-router-dom';

interface DevelopmentIntegrationInstanceConfigurationsRedirectProps {
    children: ReactNode;
}

const DevelopmentIntegrationInstanceConfigurationsRedirect = ({
    children,
}: DevelopmentIntegrationInstanceConfigurationsRedirectProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const location = useLocation();
    const [searchParams] = useSearchParams();

    if (currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT) {
        return <>{children}</>;
    }

    const integrationId = searchParams.get('integrationId');

    return (
        <Navigate
            replace
            state={location.state}
            to={
                integrationId
                    ? `/embedded/integrations?integrationId=${integrationId}&tab=configurations`
                    : '/embedded/integrations'
            }
        />
    );
};

export default DevelopmentIntegrationInstanceConfigurationsRedirect;
