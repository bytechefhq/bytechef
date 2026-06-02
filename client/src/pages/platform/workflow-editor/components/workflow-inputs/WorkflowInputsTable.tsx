import Button from '@/components/Button/Button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {WorkflowInput, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {EditIcon, Trash2Icon} from 'lucide-react';

interface WorkflowInputsTableProps {
    internalOnlyVisible: boolean;
    openDeleteDialog: (index: number) => void;
    openEditDialog: (index?: number) => void;
    workflowInputs: WorkflowInput[];
    workflowTestConfigurationInputs?: WorkflowTestConfiguration['inputs'];
}

// A component-property input's test value is a nested object ({member: value}); render its member
// values rather than the default "[object Object]".
const formatTestValue = (value: unknown): string => {
    if (value == null) {
        return '';
    }

    if (typeof value === 'object') {
        return Object.values(value as Record<string, unknown>)
            .filter((memberValue) => memberValue != null && memberValue !== '')
            .map((memberValue) => String(memberValue))
            .join(', ');
    }

    return String(value);
};

const WorkflowInputsTable = ({
    internalOnlyVisible,
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

                {internalOnlyVisible && <TableHead>Internal only</TableHead>}

                <TableHead>Test Value</TableHead>

                <TableHead>Actions</TableHead>
            </TableRow>
        </TableHeader>

        <TableBody>
            {workflowInputs?.map((input, index) => (
                <TableRow className="cursor-pointer border-b-border/50" key={`${input.name}-${index}`}>
                    <TableCell>{input.name}</TableCell>

                    <TableCell>{input.label}</TableCell>

                    <TableCell>{input.componentReference ? 'component' : input.type}</TableCell>

                    <TableCell>{input.required === true ? 'true' : 'false'}</TableCell>

                    {internalOnlyVisible && <TableCell>{input.internalOnly === true ? 'true' : 'false'}</TableCell>}

                    <TableCell>{formatTestValue(workflowTestConfigurationInputs?.[input.name])}</TableCell>

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
