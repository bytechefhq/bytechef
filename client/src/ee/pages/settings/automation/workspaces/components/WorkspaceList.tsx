import WorkspaceListItem from '@/ee/pages/settings/automation/workspaces/components/WorkspaceListItem';
import useOpenWorkspaceProjects from '@/ee/pages/settings/automation/workspaces/hooks/useOpenWorkspaceProjects';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Workspace} from '@/shared/middleware/automation/configuration';
import {useState} from 'react';

const WorkspaceList = ({workspaces}: {workspaces: Workspace[]}) => {
    const [previousCurrentWorkspaceId, setPreviousCurrentWorkspaceId] = useState<number | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const openWorkspaceProjects = useOpenWorkspaceProjects();

    const displayedCurrentWorkspaceId = previousCurrentWorkspaceId ?? currentWorkspaceId;

    const handleWorkspaceOpen = (workspaceId: number) => {
        setPreviousCurrentWorkspaceId(currentWorkspaceId);

        openWorkspaceProjects(workspaceId);
    };

    return (
        <ul className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5" role="list">
            {workspaces.map((workspace) => {
                return (
                    <WorkspaceListItem
                        isCurrentWorkspace={workspace.id === displayedCurrentWorkspaceId}
                        key={workspace.id}
                        onOpen={handleWorkspaceOpen}
                        workspace={workspace}
                    />
                );
            })}
        </ul>
    );
};

export default WorkspaceList;
