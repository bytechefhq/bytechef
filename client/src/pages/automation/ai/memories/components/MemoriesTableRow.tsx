import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {EllipsisVerticalIcon, PencilIcon, Trash2Icon} from 'lucide-react';
import {ReactNode, useMemo} from 'react';

import {AiAutoMemoryI, getAiAutoMemoryTypeLabel} from '../hooks/useAiAutoMemories';
import {formatRelative} from './memoriesTableUtils';

interface MemoriesTableRowPropsI<MemoryType extends AiAutoMemoryI> {
    /** The page's own cells, rendered between the Type and Updated columns. */
    children: ReactNode;
    memory: MemoryType;
    mutable: boolean;
    onDelete: (memory: MemoryType) => void;
    onEdit: (memory: MemoryType) => void;
    onView: (memory: MemoryType) => void;
}

const MemoriesTableRow = <MemoryType extends AiAutoMemoryI>({
    children,
    memory,
    mutable,
    onDelete,
    onEdit,
    onView,
}: MemoriesTableRowPropsI<MemoryType>) => {
    const relativeTime = useMemo(() => formatRelative(memory.updatedAt), [memory.updatedAt]);

    return (
        <tr
            className="cursor-pointer border-b last:border-0 hover:bg-muted/40 focus-visible:bg-muted/40 focus-visible:outline-none"
            onClick={() => onView(memory)}
            onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();

                    onView(memory);
                }
            }}
            tabIndex={0}
        >
            <td className="px-4 py-2">
                <div className="flex flex-col">
                    <span className="text-sm font-medium">{memory.title}</span>

                    <span className="text-xs text-muted-foreground">{memory.name}</span>
                </div>
            </td>

            <td className="px-4 py-2">
                <Badge label={getAiAutoMemoryTypeLabel(memory.memoryType)} styleType="secondary-outline" />
            </td>

            {children}

            <td className="px-4 py-2 text-sm text-muted-foreground">{relativeTime}</td>

            <td className="w-px px-4 py-2 text-right">
                {mutable && (
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild onClick={(event) => event.stopPropagation()}>
                            <Button
                                aria-label={`More actions for ${memory.title}`}
                                icon={<EllipsisVerticalIcon />}
                                size="icon"
                                variant="ghost"
                            />
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end" onClick={(event) => event.stopPropagation()}>
                            <DropdownMenuItem icon={<PencilIcon />} label="Edit" onClick={() => onEdit(memory)} />

                            <DropdownMenuSeparator />

                            <DropdownMenuItem
                                icon={<Trash2Icon />}
                                label="Delete"
                                onClick={() => onDelete(memory)}
                                variant="destructive"
                            />
                        </DropdownMenuContent>
                    </DropdownMenu>
                )}
            </td>
        </tr>
    );
};

export default MemoriesTableRow;
