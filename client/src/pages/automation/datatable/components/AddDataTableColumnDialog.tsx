import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {ColumnType} from '@/shared/middleware/graphql';
import {useEffect, useState} from 'react';

import useAddDataTableColumnDialog from '../hooks/useAddDataTableColumnDialog';

const COLUMN_TYPES: ColumnType[] = [
    ColumnType.String,
    ColumnType.Number,
    ColumnType.Integer,
    ColumnType.Date,
    ColumnType.DateTime,
    ColumnType.Boolean,
];

const AddDataTableColumnDialog = () => {
    const [columnName, setColumnName] = useState('');
    const [columnType, setColumnType] = useState<ColumnType>(ColumnType.String);

    const {handleAdd, handleOpenChange, open} = useAddDataTableColumnDialog();

    useEffect(() => {
        if (!open) {
            setColumnName('');
            setColumnType(ColumnType.String);
        }
    }, [open]);

    const trimmedColumnName = columnName.trim();

    const isReservedName = trimmedColumnName.toLowerCase() === 'id';

    const isValidColumnName = trimmedColumnName.length > 0 && !isReservedName;

    const handleAddClick = () => {
        if (!isValidColumnName) {
            return;
        }

        handleAdd(trimmedColumnName, columnType);
    };

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent>
                <DialogMain>
                    <DialogHeader description="Enter a new name for the column." title="Add Column" />

                    <DialogBody>
                        <div className="flex flex-col gap-4">
                            <div className="flex flex-col gap-1.5">
                                <Label>Name</Label>

                                <Input onChange={(event) => setColumnName(event.target.value)} value={columnName} />

                                {isReservedName && (
                                    <p className="text-sm text-destructive">&quot;id&quot; is a reserved column name</p>
                                )}
                            </div>

                            <div className="flex flex-col gap-1.5">
                                <Label>Type</Label>

                                <Select
                                    onValueChange={(value) => setColumnType(value as ColumnType)}
                                    value={columnType}
                                >
                                    <SelectTrigger className="w-[240px]">
                                        <SelectValue placeholder="Select type" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        {COLUMN_TYPES.map((type) => (
                                            <SelectItem key={type} value={type}>
                                                {type}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button disabled={!isValidColumnName} label="Add" onClick={handleAddClick} />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AddDataTableColumnDialog;
