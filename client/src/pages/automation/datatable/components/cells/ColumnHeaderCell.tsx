import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {LinkIcon, MoreVerticalIcon} from 'lucide-react';

interface ColumnHeaderCellProps {
    columnId: string;
    columnName: string;
    linkTargetTableName?: string;
    onDelete: (columnId: string, columnName: string) => void;
    onRename: (columnId: string, columnName: string) => void;
    readOnly?: boolean;
}

const ColumnHeaderCell = ({
    columnId,
    columnName,
    linkTargetTableName,
    onDelete,
    onRename,
    readOnly = false,
}: ColumnHeaderCellProps) => {
    return (
        <div className="relative flex items-center justify-center gap-1 pr-8">
            {linkTargetTableName && (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span aria-label={`Links to ${linkTargetTableName}`} className="flex shrink-0" role="img">
                            <LinkIcon className="size-3.5 text-muted-foreground" />
                        </span>
                    </TooltipTrigger>

                    <TooltipContent>{linkTargetTableName}</TooltipContent>
                </Tooltip>
            )}

            <span className="truncate text-center" title={columnName}>
                {columnName}
            </span>

            {!readOnly && (
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
            )}
        </div>
    );
};

export default ColumnHeaderCell;
