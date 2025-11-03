import Button from '@/components/Button/Button';
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
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import WorkflowOutputsSheetDialog from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheetDialog';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {Workflow, WorkflowInput} from '@/shared/middleware/platform/configuration';
import {WorkflowDefinitionType} from '@/shared/types';
import {CableIcon, EditIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import WorkflowOutputValue from './WorkflowOutputValue';

const SPACE = 4;

const WorkflowOutputsSheetTable = ({workflow}: {workflow: Workflow}) => {
    const [currentInputIndex, setCurrentInputIndex] = useState<number>(-1);
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const componentDefinitions = useWorkflowDataStore((state) => state.componentDefinitions);

    const {updateWorkflowMutation} = useWorkflowEditor();

    function handleDelete(input: WorkflowInput) {
        const definitionObject: WorkflowDefinitionType = JSON.parse(workflow.definition!);

        const outputs: WorkflowInput[] = definitionObject.outputs ?? [];

        const index = outputs.findIndex((curInput) => curInput.name === input.name);

        outputs.splice(index, 1);

        updateWorkflowMutation!.mutate({
            id: workflow.id!,
            workflow: {
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
                        <TableRow className="border-b-border/50">
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
                                <TableRow className="cursor-pointer border-b-border/50" key={output.name}>
                                    <TableCell>{output.name}</TableCell>

                                    <TableCell>
                                        <WorkflowOutputValue
                                            componentDefinitions={componentDefinitions}
                                            value={output.value.toString()}
                                        />
                                    </TableCell>

                                    <TableCell className="flex justify-end">
                                        <Button
                                            icon={<EditIcon />}
                                            onClick={() => {
                                                setCurrentInputIndex(index);
                                                setShowEditDialog(true);
                                            }}
                                            size="icon"
                                            variant="ghost"
                                        />

                                        <Button
                                            icon={<Trash2Icon className="text-destructive" />}
                                            onClick={() => {
                                                setCurrentInputIndex(index);
                                                setShowDeleteDialog(true);
                                            }}
                                            size="icon"
                                            variant="ghost"
                                        />
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
                                triggerNode={<Button label="New Output" size="sm" />}
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
                            className="bg-destructive"
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
