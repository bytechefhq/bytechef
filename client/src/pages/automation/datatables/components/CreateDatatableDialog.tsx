import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import useCreateDataTableDialog from '@/pages/automation/datatables/components/hooks/useCreateDataTableDialog';
import {ColumnType} from '@/shared/middleware/graphql';
import {Plus, Trash2} from 'lucide-react';
import {ReactNode} from 'react';

interface Props {
    trigger?: ReactNode;
}

const CreateDatatableDialog = ({trigger}: Props) => {
    const {
        baseName,
        canSubmit,
        columns,
        description,
        handleAddColumn,
        handleBaseNameChange,
        handleColumnNameChange,
        handleColumnTypeChange,
        handleCreate,
        handleDescriptionChange,
        handleOpen,
        handleOpenChange,
        handleRemoveColumn,
        isPending,
        open,
    } = useCreateDataTableDialog();

    const handleDialogOpenChange = (isOpen: boolean) => {
        if (isOpen) {
            handleOpen();
        } else {
            handleOpenChange(isOpen);
        }
    };

    return (
        <Dialog onOpenChange={handleDialogOpenChange} open={open}>
            <DialogTrigger asChild>{trigger ?? <Button label="Create Table" />}</DialogTrigger>

            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Create Data Table</DialogTitle>

                    <DialogDescription>
                        Provide a base name, an optional description, and at least one column.
                    </DialogDescription>
                </DialogHeader>

                <div className="space-y-4">
                    <div className="space-y-2">
                        <Label htmlFor="baseName">Base name</Label>

                        <Input
                            id="baseName"
                            onChange={(event) => handleBaseNameChange(event.target.value)}
                            placeholder="e.g., orders"
                            value={baseName}
                        />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="description">Description (optional)</Label>

                        <Input
                            id="description"
                            onChange={(event) => handleDescriptionChange(event.target.value)}
                            placeholder="e.g., Stores all orders placed by customers"
                            value={description}
                        />
                    </div>

                    <div className="space-y-2">
                        <div className="flex items-center justify-between">
                            <Label>Columns</Label>

                            <Button
                                icon={<Plus />}
                                label="Add column"
                                onClick={handleAddColumn}
                                size="sm"
                                variant="secondary"
                            />
                        </div>

                        <div className="space-y-3">
                            {columns.map((column, index) => (
                                <div className="grid grid-cols-12 items-center gap-2" key={index}>
                                    <Input
                                        className="col-span-7"
                                        onChange={(event) => handleColumnNameChange(index, event.target.value)}
                                        placeholder="Column name"
                                        value={column.name}
                                    />

                                    <div className="col-span-4">
                                        <Select
                                            onValueChange={(value) =>
                                                handleColumnTypeChange(index, value as ColumnType)
                                            }
                                            value={column.type}
                                        >
                                            <SelectTrigger>
                                                <SelectValue placeholder="Type" />
                                            </SelectTrigger>

                                            <SelectContent>
                                                <SelectItem value={ColumnType.String}>STRING</SelectItem>

                                                <SelectItem value={ColumnType.Number}>NUMBER</SelectItem>

                                                <SelectItem value={ColumnType.Integer}>INTEGER</SelectItem>

                                                <SelectItem value={ColumnType.Date}>DATE</SelectItem>

                                                <SelectItem value={ColumnType.DateTime}>DATE_TIME</SelectItem>

                                                <SelectItem value={ColumnType.Boolean}>BOOLEAN</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </div>

                                    <div className="col-span-1 flex justify-end">
                                        {columns.length > 1 && (
                                            <Button
                                                icon={<Trash2 />}
                                                onClick={() => handleRemoveColumn(index)}
                                                size="icon"
                                                variant="ghost"
                                            />
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                <DialogFooter>
                    <Button
                        disabled={!canSubmit || isPending}
                        label={isPending ? 'Creating...' : 'Create'}
                        onClick={handleCreate}
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default CreateDatatableDialog;
