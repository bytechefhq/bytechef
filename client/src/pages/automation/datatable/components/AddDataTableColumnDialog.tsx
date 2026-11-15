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
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {ColumnType, LinkDeleteRule} from '@/shared/middleware/graphql';
import {useEffect, useMemo, useState} from 'react';

import useAddDataTableColumnDialog from '../hooks/useAddDataTableColumnDialog';

const COLUMN_TYPES: ColumnType[] = [
    ColumnType.String,
    ColumnType.Number,
    ColumnType.Integer,
    ColumnType.Date,
    ColumnType.DateTime,
    ColumnType.Boolean,
    ColumnType.Link,
];

const NO_DISPLAY_COLUMN = '__none__';

const AddDataTableColumnDialog = () => {
    const [columnName, setColumnName] = useState('');
    const [columnType, setColumnType] = useState<ColumnType>(ColumnType.String);
    const [displayColumnName, setDisplayColumnName] = useState(NO_DISPLAY_COLUMN);
    const [onDelete, setOnDelete] = useState<LinkDeleteRule>(LinkDeleteRule.Block);
    const [targetTableId, setTargetTableId] = useState('');

    const {handleAdd, handleOpenChange, open, targetTables} = useAddDataTableColumnDialog();

    const isLink = columnType === ColumnType.Link;

    const displayColumns = useMemo(
        () =>
            (targetTables.find((table) => table.id === targetTableId)?.columns ?? []).filter(
                (column) => column.type !== ColumnType.Link
            ),
        [targetTableId, targetTables]
    );

    useEffect(() => {
        if (!open) {
            setColumnName('');
            setColumnType(ColumnType.String);
            setDisplayColumnName(NO_DISPLAY_COLUMN);
            setOnDelete(LinkDeleteRule.Block);
            setTargetTableId('');
        }
    }, [open]);

    const trimmedColumnName = columnName.trim();

    const isReservedName = trimmedColumnName.toLowerCase() === 'id';

    const isValidColumnName = trimmedColumnName.length > 0 && !isReservedName && (!isLink || targetTableId !== '');

    const handleAddClick = () => {
        if (!isValidColumnName) {
            return;
        }

        if (isLink) {
            handleAdd(trimmedColumnName, columnType, {
                displayColumnName: displayColumnName === NO_DISPLAY_COLUMN ? undefined : displayColumnName,
                onDelete,
                targetDataTableId: targetTableId,
            });

            return;
        }

        handleAdd(trimmedColumnName, columnType);
    };

    const handleTargetTableChange = (value: string) => {
        setTargetTableId(value);
        setDisplayColumnName(NO_DISPLAY_COLUMN);
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
                                    <SelectTrigger aria-label="Type" className="w-[240px]">
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

                            {isLink && (
                                <>
                                    <div className="flex flex-col gap-1.5">
                                        <Label>Target table</Label>

                                        <Select onValueChange={handleTargetTableChange} value={targetTableId}>
                                            <SelectTrigger aria-label="Target table" className="w-[240px]">
                                                <SelectValue placeholder="Select table" />
                                            </SelectTrigger>

                                            <SelectContent>
                                                {targetTables.map((table) => (
                                                    <SelectItem key={table.id} value={table.id}>
                                                        {table.baseName}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    </div>

                                    <div className="flex flex-col gap-1.5">
                                        <Label>Display column</Label>

                                        <Select
                                            disabled={targetTableId === ''}
                                            onValueChange={setDisplayColumnName}
                                            value={displayColumnName}
                                        >
                                            <SelectTrigger aria-label="Display column" className="w-[240px]">
                                                <SelectValue placeholder="None" />
                                            </SelectTrigger>

                                            <SelectContent>
                                                <SelectItem value={NO_DISPLAY_COLUMN}>None</SelectItem>

                                                {displayColumns.map((column) => (
                                                    <SelectItem key={column.id} value={column.name}>
                                                        {column.name}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    </div>

                                    <fieldset className="flex flex-col gap-1.5 border-0 p-0">
                                        <legend className="mb-1.5 text-sm font-medium">
                                            When a linked row is deleted
                                        </legend>

                                        <RadioGroup
                                            onValueChange={(value) => setOnDelete(value as LinkDeleteRule)}
                                            value={onDelete}
                                        >
                                            <div className="flex items-center gap-2">
                                                <RadioGroupItem id="linkDeleteBlock" value={LinkDeleteRule.Block} />

                                                <Label htmlFor="linkDeleteBlock">Prevent deleting linked rows</Label>
                                            </div>

                                            <div className="flex items-center gap-2">
                                                <RadioGroupItem id="linkDeleteClear" value={LinkDeleteRule.Clear} />

                                                <Label htmlFor="linkDeleteClear">Clear the link</Label>
                                            </div>
                                        </RadioGroup>
                                    </fieldset>
                                </>
                            )}
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
