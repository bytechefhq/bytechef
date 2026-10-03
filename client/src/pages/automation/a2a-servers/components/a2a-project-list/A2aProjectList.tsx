import {A2aServer} from '@/shared/middleware/graphql';

import A2aProjectListItem from './A2aProjectListItem';
import useA2aProjectList from './hooks/useA2aProjectList';

interface A2aProjectListProps {
    a2aServer: A2aServer;
}

const A2aProjectList = ({a2aServer}: A2aProjectListProps) => {
    const {a2aProjects, isError, isLoading} = useA2aProjectList(a2aServer.id);

    if (isLoading) {
        return <p className="text-xs text-muted-foreground">Loading projects...</p>;
    }

    if (isError) {
        return <p className="text-xs text-content-destructive">The projects of this server could not be loaded.</p>;
    }

    if (!a2aProjects.length) {
        return (
            <p className="text-xs text-muted-foreground">
                No projects yet. Use Add Project to expose agent-backed workflows as skills.
            </p>
        );
    }

    return (
        <div className="flex flex-col gap-1.5">
            {a2aProjects.map((a2aProject) => (
                <A2aProjectListItem a2aProject={a2aProject} a2aServer={a2aServer} key={a2aProject.id} />
            ))}
        </div>
    );
};

export default A2aProjectList;
