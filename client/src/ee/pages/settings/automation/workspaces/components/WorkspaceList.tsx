import WorkspaceListItem from '@/ee/pages/settings/automation/workspaces/components/WorkspaceListItem';
import {Workspace} from '@/shared/middleware/automation/configuration';

const WorkspaceList = ({workspaces}: {workspaces: Workspace[]}) => {
    return (
        <ul className="w-full px-4 3xl:mx-auto 3xl:w-4/5" role="list">
            {workspaces.map((workspace) => {
                return <WorkspaceListItem key={workspace.id} workspace={workspace} />;
            })}
        </ul>
    );
};

export default WorkspaceList;
