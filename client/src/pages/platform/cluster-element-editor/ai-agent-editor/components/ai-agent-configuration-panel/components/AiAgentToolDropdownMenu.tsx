import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {EllipsisVerticalIcon, SettingsIcon, TrashIcon} from 'lucide-react';

import useAiAgentToolDropdownMenu from './hooks/useAiAgentToolDropdownMenu';
import {ToolItemI} from './hooks/useAiAgentTools';

interface AiAgentToolDropdownMenuProps {
    tool: ToolItemI;
}

export default function AiAgentToolDropdownMenu({tool}: AiAgentToolDropdownMenuProps) {
    const {handleConfigureTool, handleRemoveTool} = useAiAgentToolDropdownMenu();

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button className="h-6" size="sm" variant="ghost">
                    <EllipsisVerticalIcon className="size-3 text-gray-400" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => handleConfigureTool(tool)}>
                    <SettingsIcon />
                    Configure
                </DropdownMenuItem>

                <DropdownMenuItem
                    className="text-destructive focus:text-destructive"
                    onClick={() => handleRemoveTool(tool)}
                >
                    <TrashIcon />
                    Remove
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
