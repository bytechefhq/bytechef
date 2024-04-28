import {Sheet, SheetContent} from '@/components/ui/sheet';
import useProjectInstanceWorkflowSheetStore from '@/pages/automation/project-instances/stores/useProjectInstanceWorkflowSheetStore';
import {useGetWorkflowQuery} from '@/queries/automation/workflows.queries';

const ProjectInstanceWorkflowSheet = () => {
    const {projectInstanceWorkflowSheetOpen, setProjectInstanceWorkflowSheetOpen, workflowId} =
        useProjectInstanceWorkflowSheetStore();

    const {data: workflow} = useGetWorkflowQuery(workflowId!, !!workflowId);

    return (
        <Sheet
            onOpenChange={() => setProjectInstanceWorkflowSheetOpen(!projectInstanceWorkflowSheetOpen)}
            open={projectInstanceWorkflowSheetOpen}
        >
            <SheetContent className="flex w-11/12 gap-0 p-0 sm:max-w-screen-xl">
                {/*TODO WorkflowEditor in readonly mode*/}

                {workflow?.label}
            </SheetContent>
        </Sheet>
    );
};

export default ProjectInstanceWorkflowSheet;
