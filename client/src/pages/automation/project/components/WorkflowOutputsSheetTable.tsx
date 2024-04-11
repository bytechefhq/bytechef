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
import {WorkflowInputModel, WorkflowModel} from '@/middleware/platform/configuration';
import {useUpdateWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import WorkflowOutputsSheetDialog from '@/pages/automation/project/components/WorkflowOutputsSheetDialog';
import {WorkflowKeys} from '@/queries/automation/workflows.queries';
import {WorkflowDefinitionType} from '@/types/types';
import {useQueryClient} from '@tanstack/react-query';
import {CableIcon, EditIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

const SPACE = 4;

const WorkflowOutputsSheetTable = ({projectId, workflow}: {projectId: number; workflow: WorkflowModel}) => {
    const [currentInputIndex, setCurrentInputIndex] = useState<number>(-1);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.projectWorkflows(projectId),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    function handleDelete(input: WorkflowInputModel) {
        const definitionObject: WorkflowDefinitionType = JSON.parse(workflow.definition!);

        const outputs: WorkflowInputModel[] = definitionObject.outputs ?? [];

        const index = outputs.findIndex((curInput) => curInput.name === input.name);

        outputs.splice(index, 1);

        updateWorkflowMutation.mutate({
            id: workflow.id!,
            workflowModel: {
                definition: JSON.stringify(
                    {
                        ...definitionObject,
                        outputs,
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
            {workflow.outputs && workflow.outputs.length > 0 ? (
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Name</TableHead>

                            <TableHead>Value</TableHead>

                            <TableHead>
                                <span className="sr-only">Edit</span>
                            </TableHead>
                        </TableRow>
                    </TableHeader>

                    <TableBody>
                        {workflow.outputs &&
                            workflow.outputs.map((output, index) => (
                                <TableRow key={output.name}>
                                    <TableCell className="p-3">{output.name}</TableCell>

                                    <TableCell className="p-3">{output.value.toString()}</TableCell>

                                    <TableCell className="flex justify-end p-3">
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
                                            <Trash2Icon className="h-4 text-red-600" />
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))}
                    </TableBody>
                </Table>
            ) : (
                <div className="flex h-full flex-col justify-center">
                    <div className="flex flex-col items-center self-center align-middle">
                        <CableIcon className="size-24 text-gray-300" />

                        <h3 className="mt-2 text-sm font-semibold">No outputs</h3>

                        <p className="mt-1 text-sm text-gray-500">Get started by creating a new input.</p>

                        <div className="mt-6">
                            <WorkflowOutputsSheetDialog
                                projectId={projectId}
                                triggerNode={<Button size="sm">New Output</Button>}
                                workflow={workflow}
                            />
                        </div>
                    </div>
                </div>
            )}

            {showEditDialog && (
                <WorkflowOutputsSheetDialog
                    onClose={() => setShowEditDialog(false)}
                    outputIndex={currentInputIndex}
                    projectId={projectId}
                    workflow={workflow}
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
                            className="bg-red-600"
                            onClick={() => handleDelete(workflow.outputs![currentInputIndex]!)}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </>
    );
};

export default WorkflowOutputsSheetTable;
