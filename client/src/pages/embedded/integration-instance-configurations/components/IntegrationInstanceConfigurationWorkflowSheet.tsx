import LoadingIcon from '@/components/LoadingIcon';
import {Sheet, SheetContent} from '@/components/ui/sheet';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import ReadOnlyWorkflowEditor from '@/shared/components/ReadOnlyWorkflow';
import {useGetWorkflowQuery} from '@/shared/queries/embedded/workflows.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';

const IntegrationInstanceConfigurationWorkflowSheet = () => {
    const {
        integrationInstanceConfigurationWorkflowSheetOpen,
        setIntegrationInstanceConfigurationWorkflowSheetOpen,
        workflowId,
    } = useIntegrationInstanceConfigurationWorkflowSheetStore();

    const {data: workflow} = useGetWorkflowQuery(workflowId!, !!workflowId);

    const workflowComponentNames = [
        ...(workflow?.workflowTriggerComponentNames ?? []),
        ...(workflow?.workflowTaskComponentNames ?? []),
    ];

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery(
        {include: workflowComponentNames},
        workflowComponentNames !== undefined
    );

    return (
        <Sheet
            onOpenChange={() =>
                setIntegrationInstanceConfigurationWorkflowSheetOpen(!integrationInstanceConfigurationWorkflowSheetOpen)
            }
            open
        >
            <SheetContent className="flex flex-col  bg-gray-100 p-4 sm:max-w-[780px]">
                <h1 className="text-lg font-semibold">
                    {workflow?.label} <span className="text-sm font-normal text-gray-500">(read-only)</span>
                </h1>

                {componentDefinitions && workflow ? (
                    <ReadOnlyWorkflowEditor componentDefinitions={componentDefinitions} workflow={workflow} />
                ) : (
                    <LoadingIcon />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default IntegrationInstanceConfigurationWorkflowSheet;
