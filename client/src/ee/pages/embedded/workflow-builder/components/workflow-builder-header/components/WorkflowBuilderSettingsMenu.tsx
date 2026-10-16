import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {EditIcon, SettingsIcon} from 'lucide-react';
import {useState} from 'react';

interface WorkflowBuilderSettingsMenuProps {
    onEditWorkflowClick: () => void;
}

const WorkflowBuilderSettingsMenu = ({onEditWorkflowClick}: WorkflowBuilderSettingsMenuProps) => {
    const [open, setOpen] = useState(false);

    return (
        <DropdownMenu onOpenChange={setOpen} open={open}>
            <Tooltip>
                <DropdownMenuTrigger
                    asChild
                    className="cursor-pointer data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                >
                    <TooltipTrigger asChild>
                        <Button
                            aria-label="Settings"
                            className="hover:bg-surface-neutral-secondary-hover [&_svg]:size-5"
                            icon={<SettingsIcon />}
                            size="icon"
                            variant="ghost"
                        />
                    </TooltipTrigger>
                </DropdownMenuTrigger>

                <TooltipContent>Settings</TooltipContent>
            </Tooltip>

            <DropdownMenuContent align="end">
                <DropdownMenuItem
                    icon={<EditIcon />}
                    label="Edit"
                    onClick={() => {
                        setOpen(false);
                        onEditWorkflowClick();
                    }}
                />
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default WorkflowBuilderSettingsMenu;
