import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ReactNode} from 'react';
import {Navigate, useSearchParams} from 'react-router-dom';

interface DevelopmentAutomationWorkflowConfigurationsRedirectProps {
    children: ReactNode;
}

const DevelopmentAutomationWorkflowConfigurationsRedirect = ({
    children,
}: DevelopmentAutomationWorkflowConfigurationsRedirectProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const [searchParams] = useSearchParams();

    if (currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT) {
        return <>{children}</>;
    }

    const projectId = searchParams.get('projectId');

    return (
        <Navigate
            replace
            to={
                projectId
                    ? `/embedded/automation-workflows?projectId=${projectId}&tab=configurations`
                    : '/embedded/automation-workflows'
            }
        />
    );
};

export default DevelopmentAutomationWorkflowConfigurationsRedirect;
