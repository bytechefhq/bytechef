import LoadingIcon from '@/components/LoadingIcon';
import {Sheet, SheetContent} from '@/components/ui/sheet';
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

export default ReadOnlyWorkflowEditorSheet;
