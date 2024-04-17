import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {useGetIntegrationWorkflowsQuery} from '@/queries/embedded/integrationWorkflows.queries';

const IntegrationHeaderWorkflowSelect = ({
    integrationId,
    onValueChange,
    workflowId,
}: {
    onValueChange: (id: string) => void;
    integrationId: number;
    workflowId: string;
}) => {
    const {data: integrationWorkflows} = useGetIntegrationWorkflowsQuery(integrationId);

    return (
        <Select
            defaultValue={workflowId}
            name="integrationWorkflowSelect"
            onValueChange={onValueChange}
            value={workflowId}
        >
            <SelectTrigger className="mr-0.5 w-60 border-0 shadow-none hover:bg-gray-200">
                <SelectValue className="font-semibold" placeholder="Select a workflow" />
            </SelectTrigger>

            {integrationWorkflows && (
                <SelectContent>
                    <SelectGroup>
                        <SelectLabel>Workflows</SelectLabel>

                        {integrationWorkflows.map((workflow) => (
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

export default IntegrationHeaderWorkflowSelect;
