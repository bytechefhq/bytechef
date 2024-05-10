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
    workflowId,
}: {
    onValueChange: (id: string) => void;
    projectId: number;
    workflowId: string;
}) => {
    const {data: projectWorkflows} = useGetProjectWorkflowsQuery(projectId, !!projectId);

    return (
        <Select defaultValue={workflowId} name="projectWorkflowSelect" onValueChange={onValueChange} value={workflowId}>
            <SelectTrigger className="mr-0.5 w-60 border-0 shadow-none hover:bg-gray-200">
                <SelectValue className="font-semibold" placeholder="Select a workflow" />
            </SelectTrigger>

            {projectWorkflows && (
                <SelectContent>
                    <SelectGroup>
                        <SelectLabel>Workflows</SelectLabel>

                        {projectWorkflows.map((workflow) => (
                            <SelectItem key={workflow.id!} value={workflow.id!}>
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
