import Button from '@/components/Button/Button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {WorkflowInput, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {EditIcon, Trash2Icon} from 'lucide-react';

interface WorkflowInputsTableProps {
    internalOnlyVisible?: boolean;
    openDeleteDialog: (index: number) => void;
    openEditDialog: (index?: number) => void;
    workflowInputs: WorkflowInput[];
    workflowTestConfigurationInputs?: WorkflowTestConfiguration['inputs'];
}

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
    <Table className="table-fixed">
        <TableHeader>
            <TableRow className="border-b-border/50">
                <TableHead className="w-[16%] truncate">Name</TableHead>

                <TableHead className="w-[16%] truncate">Label</TableHead>

                <TableHead className="w-[12%] truncate">Type</TableHead>

                <TableHead className="w-[12%] truncate">Required</TableHead>

                {internalOnlyVisible && <TableHead className="w-[12%] truncate">Internal only</TableHead>}

                <TableHead className={internalOnlyVisible ? 'w-[18%] truncate' : 'w-[30%] truncate'}>
                    Test Value
                </TableHead>

                <TableHead className="w-[14%] truncate">Actions</TableHead>
            </TableRow>
        </TableHeader>

        <TableBody>
            {workflowInputs?.map((input, index) => {
                const inputType = input.componentReference ? 'component' : input.type;
                const testValue = formatTestValue(workflowTestConfigurationInputs?.[input.name]);

                return (
                    <TableRow className="cursor-pointer border-b-border/50" key={`${input.name}-${index}`}>
                        <TableCell className="truncate" title={input.name}>
                            {input.name}
                        </TableCell>

                        <TableCell className="truncate" title={input.label}>
                            {input.label}
                        </TableCell>

                        <TableCell className="truncate" title={inputType}>
                            {inputType}
                        </TableCell>

                        <TableCell>{input.required === true ? 'true' : 'false'}</TableCell>

                        {internalOnlyVisible && <TableCell>{input.internalOnly === true ? 'true' : 'false'}</TableCell>}

                        <TableCell className="truncate" title={testValue}>
                            {testValue}
                        </TableCell>

                        <TableCell className="flex justify-end">
                            <Button
                                aria-label="Edit input"
                                icon={<EditIcon />}
                                onClick={() => openEditDialog(index)}
                                size="icon"
                                variant="ghost"
                            />

                            <Button
                                aria-label="Delete input"
                                icon={<Trash2Icon className="text-destructive" />}
                                onClick={() => openDeleteDialog(index)}
                                size="icon"
                                variant="ghost"
                            />
                        </TableCell>
                    </TableRow>
                );
            })}
        </TableBody>
    </Table>
);

export default WorkflowInputsTable;
