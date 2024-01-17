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
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {WorkflowInputModel, WorkflowModel} from '@/middleware/platform/configuration';
import {useUpdateWorkflowMutation} from '@/mutations/automation/workflows.mutations';
import WorkflowInputsSheetDialog from '@/pages/automation/project/components/WorkflowInputsSheetDialog';
import {ProjectKeys} from '@/queries/automation/projects.queries';
import {WorkflowDefinition} from '@/types/types';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {AlignJustifyIcon, PlusIcon} from 'lucide-react';
import {useState} from 'react';

const SPACE = 4;

const WorkflowInputsSheetTable = ({
    inputs,
    projectId,
    workflow,
}: {
    inputs: WorkflowInputModel[];
    projectId: number;
    workflow: WorkflowModel;
}) => {
    const [currentInputIndex, setCurrentInputIndex] = useState<number>(-1);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.projectWorkflows(projectId),
            });
        },
    });

    function handleDelete(input: WorkflowInputModel) {
        const definitionObject: WorkflowDefinition = JSON.parse(workflow.definition!);

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
                <table className="min-w-full divide-y divide-gray-300">
                    <thead>
                        <tr>
                            <th
                                className="py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900 sm:pl-0"
                                scope="col"
                            >
                                Name
                            </th>

                            <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900" scope="col">
                                Label
                            </th>

                            <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900" scope="col">
                                Type
                            </th>

                            <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900" scope="col">
                                Required
                            </th>

                            <th className="relative py-3.5 pl-3 pr-4 sm:pr-0" scope="col">
                                <span className="sr-only">Edit</span>
                            </th>
                        </tr>
                    </thead>

                    <tbody className="divide-y divide-gray-200">
                        {inputs.map((input, index) => (
                            <tr key={input.name}>
                                <td className="whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 sm:pl-0">
                                    {input.name}
                                </td>

                                <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">{input.label}</td>

                                <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">{input.type}</td>

                                <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                    {input.required === true ? 'true' : 'false'}
                                </td>

                                <td className="relative whitespace-nowrap py-4 pl-3 pr-4 text-right text-sm font-medium sm:pr-0">
                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                                        </DropdownMenuTrigger>

                                        <DropdownMenuContent align="end">
                                            <DropdownMenuItem
                                                onClick={() => {
                                                    setCurrentInputIndex(index);
                                                    setShowEditDialog(true);
                                                }}
                                            >
                                                Edit
                                            </DropdownMenuItem>

                                            <DropdownMenuSeparator />

                                            <DropdownMenuItem
                                                className="text-red-600"
                                                onClick={() => {
                                                    setCurrentInputIndex(index);
                                                    setShowDeleteDialog(true);
                                                }}
                                            >
                                                Delete
                                            </DropdownMenuItem>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            ) : (
                <div className="flex h-full flex-col justify-center">
                    <div className="flex flex-col items-center self-center align-middle">
                        <AlignJustifyIcon className="size-24 text-gray-300" />

                        <h3 className="mt-2 text-sm font-semibold">No inputs</h3>

                        <p className="mt-1 text-sm text-gray-500">Get started by creating a new input.</p>

                        <div className="mt-6">
                            <WorkflowInputsSheetDialog
                                projectId={projectId}
                                triggerNode={
                                    <Button>
                                        <PlusIcon aria-hidden="true" className="-ml-0.5 mr-1.5 size-5" />
                                        Create Input
                                    </Button>
                                }
                                workflow={workflow}
                            />
                        </div>
                    </div>
                </div>
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
                            onClick={() => handleDelete(workflow.inputs![currentInputIndex]!)}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showEditDialog && (
                <WorkflowInputsSheetDialog
                    inputIndex={currentInputIndex}
                    onClose={() => setShowEditDialog(false)}
                    projectId={projectId}
                    workflow={workflow}
                />
            )}
        </>
    );
};

export default WorkflowInputsSheetTable;
