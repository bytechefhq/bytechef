import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {useGetProjectQuery} from '@/shared/queries/automation/projects.queries';

interface WorkflowTabLabelProps {
    name: string;
    onClick: () => void;
    projectId: string;
}

/**
 * Tab-strip label for a workflow tab. The tab shows only the workflow label; the tooltip adds the project name and a
 * "V<version> STATUS" badge from a cached project fetch, falling back to the workflow label alone while it loads.
 */
const WorkflowTabLabel = ({name, onClick, projectId}: WorkflowTabLabelProps) => {
    const {data: project} = useGetProjectQuery(Number(projectId), undefined, Number(projectId) > 0);

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <button className="max-w-40 truncate" onClick={onClick} type="button">
                    {name}
                </button>
            </TooltipTrigger>

            <TooltipContent>
                <span className="inline-flex items-center gap-2">
                    {project && (
                        <>
                            <span>{project.name}</span>

                            {project.lastProjectVersion != null && (
                                <Badge
                                    className="flex space-x-1 bg-surface-neutral-primary"
                                    styleType={
                                        project.lastStatus === ProjectStatus.Published
                                            ? 'success-outline'
                                            : 'outline-outline'
                                    }
                                    weight="semibold"
                                >
                                    <span>V{project.lastProjectVersion}</span>

                                    <span>{project.lastStatus}</span>
                                </Badge>
                            )}

                            <span>/</span>
                        </>
                    )}

                    <span>{name}</span>
                </span>
            </TooltipContent>
        </Tooltip>
    );
};

export default WorkflowTabLabel;
