import Button from '@/components/Button/Button';
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {XIcon} from 'lucide-react';

import useLinkCellEditor from './hooks/useLinkCellEditor';

import type {GridRowType, LinkValueI} from './types';

interface LinkCellProps {
    columnName: string;
    displayColumnName?: string | null;
    row: GridRowType;
}

interface LinkEditCellProps {
    columnName: string;
    displayColumnName?: string | null;
    environmentId: string;
    onRowChange: (row: GridRowType, commitChanges?: boolean) => void;
    row: GridRowType;
    tableId: string;
}

export const getLinkDisplayText = (value: unknown, displayColumnName?: string | null): string => {
    if (value === null || value === undefined || value === '') {
        return '';
    }

    if (typeof value !== 'object') {
        return `#${String(value)}`;
    }

    const linkValue = value as LinkValueI;
    const displayValue = displayColumnName ? linkValue.values?.[displayColumnName] : undefined;

    if (displayValue === null || displayValue === undefined || displayValue === '') {
        return `#${linkValue.id}`;
    }

    return String(displayValue);
};

export const LinkCell = ({columnName, displayColumnName, row}: LinkCellProps) => {
    const text = getLinkDisplayText((row as Record<string, unknown>)[columnName], displayColumnName);

    return (
        <span className="truncate" title={text}>
            {text}
        </span>
    );
};

export const LinkEditCell = ({
    columnName,
    displayColumnName,
    environmentId,
    onRowChange,
    row,
    tableId,
}: LinkEditCellProps) => {
    const {
        handleCancel,
        handleClear,
        handleOpenChange,
        handleSearchChange,
        handleSelect,
        isLoading,
        isOpen,
        options,
        search,
    } = useLinkCellEditor({columnName, environmentId, onRowChange, row, tableId});

    const text = getLinkDisplayText((row as Record<string, unknown>)[columnName], displayColumnName);

    return (
        <Popover onOpenChange={handleOpenChange} open={isOpen}>
            <PopoverTrigger asChild>
                <Button className="h-7 w-full justify-start px-2 text-xs" variant="outline">
                    {text || 'Select row…'}
                </Button>
            </PopoverTrigger>

            <PopoverContent align="start" className="-mt-9 w-72 p-0" onEscapeKeyDown={handleCancel} side="bottom">
                <Command shouldFilter={false}>
                    <CommandInput
                        onValueChange={handleSearchChange}
                        placeholder={`Search ${columnName}`}
                        value={search}
                    />

                    <CommandList>
                        {!isLoading && <CommandEmpty>No rows found.</CommandEmpty>}

                        <CommandGroup>
                            {options.map((option) => (
                                <CommandItem key={option.id} onSelect={() => handleSelect(option)} value={option.id}>
                                    {option.label || `#${option.id}`}
                                </CommandItem>
                            ))}
                        </CommandGroup>
                    </CommandList>
                </Command>

                <div className="border-t p-1">
                    <Button
                        className="h-7 w-full justify-start px-2 text-xs"
                        icon={<XIcon className="size-3" />}
                        label="Clear"
                        onClick={handleClear}
                        variant="ghost"
                    />
                </div>
            </PopoverContent>
        </Popover>
    );
};

interface LinkCellRendererParamsI {
    columnName: string;
    displayColumnName?: string | null;
}

interface LinkEditCellRendererParamsI extends LinkCellRendererParamsI {
    environmentId: string;
    tableId: string;
}

export const createLinkCellRenderer = ({columnName, displayColumnName}: LinkCellRendererParamsI) => {
    return ({row}: {row: GridRowType}) => (
        <LinkCell columnName={columnName} displayColumnName={displayColumnName} row={row} />
    );
};

export const createLinkEditCellRenderer = ({
    columnName,
    displayColumnName,
    environmentId,
    tableId,
}: LinkEditCellRendererParamsI) => {
    return (props: {row: GridRowType; onRowChange: (row: GridRowType, commitChanges?: boolean) => void}) => (
        <LinkEditCell
            columnName={columnName}
            displayColumnName={displayColumnName}
            environmentId={environmentId}
            onRowChange={props.onRowChange}
            row={props.row}
            tableId={tableId}
        />
    );
};
