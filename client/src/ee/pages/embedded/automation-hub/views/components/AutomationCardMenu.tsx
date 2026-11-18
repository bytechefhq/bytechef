import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {ClipboardIcon, EllipsisVerticalIcon, ExternalLinkIcon, SlidersHorizontalIcon, Trash2Icon} from 'lucide-react';

interface AutomationCardMenuProps {
    label: string;
    onCopyWebhookUrl?: () => void;
    onCustomize?: () => void;
    onEditInputs?: () => void;
    onRemove: () => void;
}

const AutomationCardMenu = ({
    label,
    onCopyWebhookUrl,
    onCustomize,
    onEditInputs,
    onRemove,
}: AutomationCardMenuProps) => (
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

            {onCopyWebhookUrl && (
                <DropdownMenuItem onClick={onCopyWebhookUrl}>
                    <ClipboardIcon /> Copy webhook URL
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
