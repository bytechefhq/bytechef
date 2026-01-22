import {Checkbox} from '@/components/ui/checkbox';

interface IndeterminateCheckboxProps {
    ariaLabel?: string;
    checked: boolean;
    indeterminate: boolean;
    onChange: (checked: boolean) => void;
}

export const IndeterminateCheckbox = ({ariaLabel, checked, indeterminate, onChange}: IndeterminateCheckboxProps) => (
    <Checkbox
        aria-label={ariaLabel}
        checked={indeterminate ? 'indeterminate' : checked}
        className="size-4 cursor-pointer"
        onCheckedChange={(value) => onChange(value === true)}
    />
);

interface SelectAllHeaderCellProps {
    allSelected: boolean;
    onToggleSelectAll: (checked: boolean) => void;
    someSelected: boolean;
}

const SelectAllHeaderCell = ({allSelected, onToggleSelectAll, someSelected}: SelectAllHeaderCellProps) => (
    <div className="flex w-full items-center justify-center">
        <IndeterminateCheckbox
            ariaLabel="Select all rows"
            checked={allSelected}
            indeterminate={someSelected}
            onChange={onToggleSelectAll}
        />
    </div>
);

export default SelectAllHeaderCell;
