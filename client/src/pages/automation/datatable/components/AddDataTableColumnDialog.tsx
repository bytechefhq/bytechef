import Button from '@/components/Button/Button';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
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
    const {handleAdd, handleOpenChange, open} = useAddDataTableColumnDialog();

    const [columnName, setColumnName] = useState('');
    const [columnType, setColumnType] = useState<ColumnType>(ColumnType.String);

    useEffect(() => {
        if (!open) {
            setColumnName('');
            setColumnType(ColumnType.String);
        }
    }, [open]);

    const handleAddClick = () => {
        if (!columnName) return;

        handleAdd(columnName, columnType);
    };

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Add Column</DialogTitle>
                </DialogHeader>

                <div className="space-y-3 py-2">
                    <div className="space-y-1">
                        <Label>Name</Label>

                        <Input onChange={(event) => setColumnName(event.target.value)} value={columnName} />
                    </div>

                    <div className="space-y-1">
                        <Label>Type</Label>

                        <Select onValueChange={(value) => setColumnType(value as ColumnType)} value={columnType}>
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

                <DialogFooter>
                    <Button onClick={() => handleOpenChange(false)} variant="outline">
                        Cancel
                    </Button>

                    <Button onClick={handleAddClick}>Add</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AddDataTableColumnDialog;
