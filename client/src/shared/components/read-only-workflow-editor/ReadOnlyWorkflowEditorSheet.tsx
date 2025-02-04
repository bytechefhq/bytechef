import LoadingIcon from '@/components/LoadingIcon';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import ReadOnlyWorkflowEditor from '@/shared/components/read-only-workflow-editor/ReadOnlyWorkflowEditor';
import useReadOnlyWorkflowEditorSheetStore from '@/shared/components/read-only-workflow-editor/stores/useReadOnlyWorkflowEditorSheetStore';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {UseQueryResult} from '@tanstack/react-query';

const ReadOnlyWorkflowEditorSheet = ({
    useGetWorkflowQuery,
}: {
    useGetWorkflowQuery: (id: string, enabled?: boolean) => UseQueryResult<Workflow, Error>;
}) => {
    const {readOnlyWorkflowEditorSheetOpen, setReadOnlyWorkflowEditorSheetOpen, workflowId} =
        useReadOnlyWorkflowEditorSheetStore();

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
            onOpenChange={() => setReadOnlyWorkflowEditorSheetOpen(!readOnlyWorkflowEditorSheetOpen)}
            open={readOnlyWorkflowEditorSheetOpen}
        >
            <SheetContent className="flex flex-col bg-white p-0 sm:max-w-workflow-read-only-project-deployment-workflow-sheet-width">
                <SheetHeader className="flex flex-row items-center justify-between space-y-0 p-4">
                    <SheetTitle>{workflow?.label}</SheetTitle>

                    <SheetCloseButton />
                </SheetHeader>

                {componentDefinitions && workflow ? (
                    <ReadOnlyWorkflowEditor componentDefinitions={componentDefinitions} workflow={workflow} />
                ) : (
                    <LoadingIcon />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default ReadOnlyWorkflowEditorSheet;
