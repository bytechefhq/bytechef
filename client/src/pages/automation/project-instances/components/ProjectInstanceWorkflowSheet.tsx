import LoadingIcon from '@/components/LoadingIcon';
import {Sheet, SheetContent} from '@/components/ui/sheet';
import useProjectInstanceWorkflowSheetStore from '@/pages/automation/project-instances/stores/useProjectInstanceWorkflowSheetStore';
import ReadOnlyWorkflowEditor from '@/shared/components/ReadOnlyWorkflow';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';

const ProjectInstanceWorkflowSheet = () => {
    const {projectInstanceWorkflowSheetOpen, setProjectInstanceWorkflowSheetOpen, workflowId} =
        useProjectInstanceWorkflowSheetStore();

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
            onOpenChange={() => setProjectInstanceWorkflowSheetOpen(!projectInstanceWorkflowSheetOpen)}
            open={projectInstanceWorkflowSheetOpen}
        >
            <SheetContent className="flex flex-col bg-white p-0 sm:max-w-[780px]">
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

export default ProjectInstanceWorkflowSheet;
