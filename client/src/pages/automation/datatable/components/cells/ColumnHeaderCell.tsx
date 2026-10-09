import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {MoreVerticalIcon} from 'lucide-react';

interface ColumnHeaderCellProps {
    columnId: string;
    columnName: string;
    onDelete: (columnId: string, columnName: string) => void;
    onRename: (columnId: string, columnName: string) => void;
}

const ColumnHeaderCell = ({columnId, columnName, onDelete, onRename}: ColumnHeaderCellProps) => {
    return (
        <div className="relative flex items-center justify-center pr-8">
            <span className="truncate text-center" title={columnName}>
                {columnName}
            </span>

            <div className="absolute right-0">
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            aria-label={`Column ${columnName} menu`}
                            icon={<MoreVerticalIcon className="size-4" />}
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem label="Rename" onClick={() => onRename(columnId, columnName)} />

                        <DropdownMenuItem
                            label="Delete"
                            onClick={() => onDelete(columnId, columnName)}
                            variant="destructive"
                        />
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>
        </div>
    );
};

export default ColumnHeaderCell;
