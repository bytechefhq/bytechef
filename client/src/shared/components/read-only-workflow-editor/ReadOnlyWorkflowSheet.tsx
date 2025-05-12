import LoadingIcon from '@/components/LoadingIcon';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import ReadOnlyWorkflow from '@/shared/components/read-only-workflow-editor/ReadOnlyWorkflow';

import useReadOnlyWorkflow from './hooks/useReadOnlyWorkflow';

const ReadOnlyWorkflowSheet = () => {
    const {closeReadOnlyWorkflowSheet, edges, isReadOnlyWorkflowSheetOpen, nodes, workflow} = useReadOnlyWorkflow();

    return (
        <Sheet
            onOpenChange={(isOpen) => {
                if (!isOpen) {
                    closeReadOnlyWorkflowSheet();
                }
            }}
            open={isReadOnlyWorkflowSheetOpen}
        >
            <SheetContent className="flex flex-col bg-white p-0 sm:max-w-workflow-read-only-project-deployment-workflow-sheet-width">
                <SheetHeader className="flex flex-row items-center justify-between space-y-0 p-3">
                    <SheetTitle>{workflow?.label}</SheetTitle>

                    <SheetCloseButton />
                </SheetHeader>

                {workflow ? (
                    <ReadOnlyWorkflow edges={edges} nodes={nodes} />
                ) : (
                    <div className="flex size-full items-center justify-center">
                        <LoadingIcon className="size-8" />
                    </div>
                )}
            </SheetContent>
        </Sheet>
    );
};

export default ReadOnlyWorkflowSheet;
