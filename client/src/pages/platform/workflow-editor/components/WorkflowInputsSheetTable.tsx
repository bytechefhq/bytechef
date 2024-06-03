import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Button} from '@/components/ui/button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import WorkflowInputsSheetDialog from '@/pages/platform/workflow-editor/components/WorkflowInputsSheetDialog';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {
    WorkflowInputModel,
    WorkflowModel,
    WorkflowTestConfigurationModel,
} from '@/shared/middleware/platform/configuration';
import {WorkflowDefinitionType} from '@/shared/types';
import {EditIcon, PlusIcon, SlidersIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

const SPACE = 4;

const WorkflowInputsSheetTable = ({
    workflow,
    workflowTestConfiguration,
}: {
    workflow: WorkflowModel;
    workflowTestConfiguration?: WorkflowTestConfigurationModel;
}) => {
    const [currentInputIndex, setCurrentInputIndex] = useState<number>(-1);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const {updateWorkflowMutation} = useWorkflowMutation();

    function handleDelete(input: WorkflowInputModel) {
        const definitionObject: WorkflowDefinitionType = JSON.parse(workflow.definition!);

        const inputs: WorkflowInputModel[] = definitionObject.inputs ?? [];

        const index = inputs.findIndex((curInput) => curInput.name === input.name);

        inputs.splice(index, 1);

        updateWorkflowMutation.mutate({
            id: workflow.id!,
            workflowModel: {
                definition: JSON.stringify(
                    {
                        ...definitionObject,
                        inputs,
                    },
                    null,
                    SPACE
                ),
                version: workflow.version,
            },
        });

        setShowDeleteDialog(false);
    }

    return (
        <>
            {workflow.inputs && workflow.inputs.length > 0 ? (
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Name</TableHead>

                            <TableHead>Label</TableHead>

                            <TableHead>Type</TableHead>

                            <TableHead>Required</TableHead>

                            <TableHead>Test Value</TableHead>

                            <TableHead>
                                <span className="sr-only">Edit</span>
                            </TableHead>
                        </TableRow>
                    </TableHeader>

                    <TableBody>
                        {workflow.inputs &&
                            workflow.inputs.map((input, index) => (
                                <TableRow key={input.name}>
                                    <TableCell className="px-3 py-4">{input.name}</TableCell>

                                    <TableCell className="px-3 py-4">{input.label}</TableCell>

                                    <TableCell className="px-3 py-4">{input.type}</TableCell>

                                    <TableCell className="px-3 py-4">
                                        {input.required === true ? 'true' : 'false'}
                                    </TableCell>

                                    <TableCell className="px-3 py-4">
                                        {workflowTestConfiguration?.inputs
                                            ? workflowTestConfiguration?.inputs[
                                                  workflow.inputs![index]?.name
                                              ]?.toString()
                                            : undefined}
                                    </TableCell>

                                    <TableCell className="flex justify-end px-3 py-4">
                                        <Button
                                            onClick={() => {
                                                setCurrentInputIndex(index);
                                                setShowEditDialog(true);
                                            }}
                                            size="icon"
                                            variant="ghost"
                                        >
                                            <EditIcon className="size-4" />
                                        </Button>

                                        <Button
                                            onClick={() => {
                                                setCurrentInputIndex(index);
                                                setShowDeleteDialog(true);
                                            }}
                                            size="icon"
                                            variant="ghost"
                                        >
                                            <Trash2Icon className="h-4 text-destructive" />
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))}
                    </TableBody>
                </Table>
            ) : (
                <div className="flex h-full flex-col justify-center">
                    <div className="flex flex-col items-center self-center align-middle">
                        <SlidersIcon className="size-24 text-gray-300" />

                        <h3 className="mt-2 text-sm font-semibold">No inputs</h3>

                        <p className="mt-1 text-sm text-gray-500">Get started by creating a new input.</p>

                        <div className="mt-6">
                            <WorkflowInputsSheetDialog
                                triggerNode={
                                    <Button>
                                        <PlusIcon aria-hidden="true" className="-ml-0.5 mr-1.5 size-5" />
                                        New Input
                                    </Button>
                                }
                                workflow={workflow}
                                workflowTestConfiguration={workflowTestConfiguration}
                            />
                        </div>
                    </div>
                </div>
            )}

            {showEditDialog && (
                <WorkflowInputsSheetDialog
                    inputIndex={currentInputIndex}
                    onClose={() => setShowEditDialog(false)}
                    workflow={workflow}
                    workflowTestConfiguration={workflowTestConfiguration}
                />
            )}

            <AlertDialog open={showDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the input.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setShowDeleteDialog(false)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-destructive"
                            onClick={() => handleDelete(workflow.inputs![currentInputIndex]!)}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </>
    );
};

export default WorkflowInputsSheetTable;
