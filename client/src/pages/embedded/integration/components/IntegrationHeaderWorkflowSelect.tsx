import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {useGetIntegrationWorkflowsQuery} from '@/shared/queries/embedded/integrationWorkflows.queries';

const IntegrationHeaderWorkflowSelect = ({
    integrationId,
    integrationWorkflowId,
    onValueChange,
}: {
    integrationId: number;
    integrationWorkflowId: number;
    onValueChange: (integrationWorkflowId: number) => void;
}) => {
    const {data: integrationWorkflows} = useGetIntegrationWorkflowsQuery(integrationId);

    return (
        <Select
            defaultValue={integrationWorkflowId.toString()}
            name="integrationWorkflowSelect"
            onValueChange={(value) => onValueChange(+value)}
            value={integrationWorkflowId.toString()}
        >
            <SelectTrigger className="mr-0.5 w-60 border-0 shadow-none hover:bg-gray-200">
                <SelectValue className="font-semibold" placeholder="Select a workflow" />
            </SelectTrigger>

            {integrationWorkflows && (
                <SelectContent>
                    <SelectGroup>
                        <SelectLabel>Workflows</SelectLabel>

                        {integrationWorkflows.map((workflow) => (
                            <SelectItem
                                key={workflow.integrationWorkflowId!}
                                value={workflow.integrationWorkflowId!.toString()}
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

export default IntegrationHeaderWorkflowSelect;
