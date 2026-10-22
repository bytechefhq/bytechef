import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useCallback} from 'react';
import {flushSync} from 'react-dom';
import {useNavigate} from 'react-router-dom';

interface UseOpenSavedProjectDeploymentProps {
    onDevelopmentDeploymentSaved: (projectDeploymentId: number) => void;
    projectId?: number;
}

export default function useOpenSavedProjectDeployment({
    onDevelopmentDeploymentSaved,
    projectId,
}: UseOpenSavedProjectDeploymentProps) {
    const setCurrentEnvironmentId = useEnvironmentStore((state) => state.setCurrentEnvironmentId);

    const navigate = useNavigate();

    return useCallback(
        (projectDeploymentId: number, environmentId?: number) => {
            if (environmentId === undefined || environmentId === DEVELOPMENT_ENVIRONMENT) {
                onDevelopmentDeploymentSaved(projectDeploymentId);

                return;
            }

            flushSync(() => setCurrentEnvironmentId(environmentId));

            const search = projectId != null ? `?projectId=${projectId}` : '';

            void navigate(`/automation/deployments${search}`, {state: {projectDeploymentId}});
        },
        [navigate, onDevelopmentDeploymentSaved, projectId, setCurrentEnvironmentId]
    );
}
