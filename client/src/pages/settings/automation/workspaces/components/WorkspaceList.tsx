import WorkspaceListItem from '@/pages/settings/automation/workspaces/components/WorkspaceListItem';
import {WorkspaceModel} from '@/shared/middleware/automation/configuration';

const WorkspaceList = ({workspaces}: {workspaces: WorkspaceModel[]}) => {
    return (
        <ul className="w-full divide-y divide-gray-100 px-2 2xl:mx-auto 2xl:w-4/5" role="list">
            {workspaces.map((workspace) => {
                return <WorkspaceListItem key={workspace.id} workspace={workspace} />;
            })}
        </ul>
    );
};

export default WorkspaceList;
