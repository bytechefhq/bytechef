import LoadingIcon from '@/components/LoadingIcon';
import {Sheet, SheetContent} from '@/components/ui/sheet';
import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import ReadOnlyWorkflowEditor from '@/shared/components/read-only-workflow-editor/ReadOnlyWorkflowEditor';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';

const ProjectDeploymentWorkflowSheet = () => {
    const {projectDeploymentWorkflowSheetOpen, setProjectDeploymentWorkflowSheetOpen, workflowId} =
        useProjectDeploymentWorkflowSheetStore();

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
            onOpenChange={() => setProjectDeploymentWorkflowSheetOpen(!projectDeploymentWorkflowSheetOpen)}
            open={projectDeploymentWorkflowSheetOpen}
        >
            <SheetContent className="flex flex-col bg-white p-0 sm:max-w-workflow-read-only-project-deployment-workflow-sheet-width">
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

export default ProjectDeploymentWorkflowSheet;
