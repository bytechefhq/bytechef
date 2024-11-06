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
            open={integrationInstanceConfigurationWorkflowSheetOpen}
        >
            <SheetContent className="flex flex-col bg-white p-0 sm:max-w-workflow-read-only-project-instance-workflow-sheet-width">
                <div className="size-full bg-muted/50 p-4">
                    <h1 className="text-lg font-semibold">{workflow?.label}</h1>

                    {componentDefinitions && workflow ? (
                        <ReadOnlyWorkflowEditor componentDefinitions={componentDefinitions} workflow={workflow} />
                    ) : (
                        <LoadingIcon />
                    )}
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default IntegrationInstanceConfigurationWorkflowSheet;
