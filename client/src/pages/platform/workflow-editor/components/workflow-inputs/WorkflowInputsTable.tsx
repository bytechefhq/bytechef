import Button from '@/components/Button/Button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {WorkflowInput, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {EditIcon, Trash2Icon} from 'lucide-react';

interface WorkflowInputsTableProps {
    openDeleteDialog: (index: number) => void;
    openEditDialog: (index?: number) => void;
    workflowInputs: WorkflowInput[];
    workflowTestConfigurationInputs?: WorkflowTestConfiguration['inputs'];
}

const WorkflowInputsTable = ({
    openDeleteDialog,
    openEditDialog,
    workflowInputs,
    workflowTestConfigurationInputs,
}: WorkflowInputsTableProps) => (
    <Table>
        <TableHeader>
            <TableRow className="border-b-border/50">
                <TableHead>Name</TableHead>

                <TableHead>Label</TableHead>

                <TableHead>Type</TableHead>

                <TableHead>Required</TableHead>

                <TableHead>Test Value</TableHead>

                <TableHead>Actions</TableHead>
            </TableRow>
        </TableHeader>

        <TableBody>
            {workflowInputs?.map((input, index) => (
                <TableRow className="cursor-pointer border-b-border/50" key={`${input.name}-${index}`}>
                    <TableCell>{input.name}</TableCell>

                    <TableCell>{input.label}</TableCell>

                    <TableCell>{input.type}</TableCell>

                    <TableCell>{input.required === true ? 'true' : 'false'}</TableCell>

                    <TableCell>
                        {workflowTestConfigurationInputs
                            ? workflowTestConfigurationInputs[workflowInputs![index]?.name]?.toString()
                            : undefined}
                    </TableCell>

                    <TableCell className="flex justify-end">
                        <Button icon={<EditIcon />} onClick={() => openEditDialog(index)} size="icon" variant="ghost" />

                        <Button
                            icon={<Trash2Icon className="text-destructive" />}
                            onClick={() => openDeleteDialog(index)}
                            size="icon"
                            variant="ghost"
                        />
                    </TableCell>
                </TableRow>
            ))}
        </TableBody>
    </Table>
);

export default WorkflowInputsTable;
