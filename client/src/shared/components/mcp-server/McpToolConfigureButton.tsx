import Button from '@/components/Button/Button';
import {PopoverAnchor} from '@/components/ui/popover';
import {BoltIcon} from 'lucide-react';

interface McpToolConfigureButtonProps {
    anchorsPopover?: boolean;
    label: string;
    onClick: () => void;
}

const McpToolConfigureButton = ({anchorsPopover = false, label, onClick}: McpToolConfigureButtonProps) => {
    const button = (
        <Button
            aria-label={`Configure ${label}`}
            className="shrink-0 text-muted-foreground hover:text-foreground"
            icon={<BoltIcon className="size-4" />}
            onClick={onClick}
            size="iconSm"
            title="Configure"
            variant="ghost"
        />
    );

    return anchorsPopover ? <PopoverAnchor asChild>{button}</PopoverAnchor> : button;
};

export default McpToolConfigureButton;
