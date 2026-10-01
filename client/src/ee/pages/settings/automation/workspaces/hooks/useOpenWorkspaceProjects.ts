import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useCallback} from 'react';
import {useNavigate} from 'react-router-dom';

export default function useOpenWorkspaceProjects() {
    const setCurrentEnvironmentId = useEnvironmentStore((state) => state.setCurrentEnvironmentId);
    const setCurrentWorkspaceId = useWorkspaceStore((state) => state.setCurrentWorkspaceId);

    const navigate = useNavigate();

    return useCallback(
        (workspaceId: number) => {
            setCurrentWorkspaceId(workspaceId);
            setCurrentEnvironmentId(DEVELOPMENT_ENVIRONMENT);

            void navigate('/automation/projects');
        },
        [navigate, setCurrentEnvironmentId, setCurrentWorkspaceId]
    );
}
