import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {useGetProjectWorkflowsQuery} from '@/queries/automation/projectWorkflows.queries';

const ProjectHeaderWorkflowSelect = ({
    onValueChange,
    projectId,
    projectWorkflowId,
}: {
    onValueChange: (projectWorkflowId: number) => void;
    projectId: number;
    projectWorkflowId: number;
}) => {
    const {data: projectWorkflows} = useGetProjectWorkflowsQuery(projectId);

    return (
        <Select
            defaultValue={projectWorkflowId.toString()}
            name="projectWorkflowSelect"
            onValueChange={(value) => onValueChange(+value)}
            value={projectWorkflowId.toString()}
        >
            <SelectTrigger className="mr-0.5 w-60 border-0 shadow-none hover:bg-gray-200">
                <SelectValue className="font-semibold" placeholder="Select a workflow" />
            </SelectTrigger>

            {projectWorkflows && (
                <SelectContent>
                    <SelectGroup>
                        <SelectLabel>Workflows</SelectLabel>

                        {projectWorkflows.map((workflow) => (
                            <SelectItem
                                key={workflow.projectWorkflowId!}
                                value={workflow.projectWorkflowId!.toString()}
                            >
                                {workflow.label!}
                            </SelectItem>
                        ))}
                    </SelectGroup>
                </SelectContent>
            )}
        </Select>
    );
};

export default ProjectHeaderWorkflowSelect;
