import {Sheet, SheetContent} from '@/components/ui/sheet';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {useGetWorkflowQuery} from '@/queries/embedded/workflows.queries';

const IntegrationInstanceConfigurationWorkflowSheet = () => {
    const {
        integrationInstanceConfigurationWorkflowSheetOpen,
        setIntegrationInstanceConfigurationWorkflowSheetOpen,
        workflowId,
    } = useIntegrationInstanceConfigurationWorkflowSheetStore();

    const {data: workflow} = useGetWorkflowQuery(workflowId!, !!workflowId);

    return (
        <Sheet
            onOpenChange={() =>
                setIntegrationInstanceConfigurationWorkflowSheetOpen(!integrationInstanceConfigurationWorkflowSheetOpen)
            }
            open={integrationInstanceConfigurationWorkflowSheetOpen}
        >
            <SheetContent className="flex w-11/12 gap-0 p-0 sm:max-w-screen-xl">
                {/*TODO WorkflowEditor in readonly mode*/}

                {workflow?.label}
            </SheetContent>
        </Sheet>
    );
};

export default IntegrationInstanceConfigurationWorkflowSheet;
