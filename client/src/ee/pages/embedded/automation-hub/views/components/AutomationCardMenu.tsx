import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {EllipsisVerticalIcon, ExternalLinkIcon, SlidersHorizontalIcon, Trash2Icon} from 'lucide-react';

interface AutomationCardMenuProps {
    label: string;
    onCustomize?: () => void;
    onEditInputs?: () => void;
    onRemove: () => void;
}

const AutomationCardMenu = ({label, onCustomize, onEditInputs, onRemove}: AutomationCardMenuProps) => (
    <DropdownMenu>
        <DropdownMenuTrigger asChild>
            <Button aria-label={`${label} actions`} icon={<EllipsisVerticalIcon />} size="iconSm" variant="ghost" />
        </DropdownMenuTrigger>

        <DropdownMenuContent align="end">
            {onEditInputs && (
                <DropdownMenuItem onClick={onEditInputs}>
                    <SlidersHorizontalIcon /> Settings
                </DropdownMenuItem>
            )}

            {onCustomize && (
                <DropdownMenuItem onClick={onCustomize}>
                    <ExternalLinkIcon /> Customize
                </DropdownMenuItem>
            )}

            <DropdownMenuItem onClick={onRemove} variant="destructive">
                <Trash2Icon /> Remove
            </DropdownMenuItem>
        </DropdownMenuContent>
    </DropdownMenu>
);

export default AutomationCardMenu;
