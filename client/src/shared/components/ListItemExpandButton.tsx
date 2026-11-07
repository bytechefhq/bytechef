import Button from '@/components/Button/Button';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {ChevronDownIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface ListItemExpandButtonProps {
    className?: string;
    disabled?: boolean;
    label: string;
}

const ListItemExpandButton = ({className, disabled, label}: ListItemExpandButtonProps) => (
    <CollapsibleTrigger asChild disabled={disabled} onClick={(event) => event.stopPropagation()}>
        <Button
            aria-label={label}
            className={twMerge(
                'group/expand mr-3 flex-none data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary',
                className
            )}
            data-interactive
            icon={<ChevronDownIcon className="duration-300 group-data-[state=open]/expand:rotate-180" />}
            size="iconXxs"
            variant="ghost"
        />
    </CollapsibleTrigger>
);

export default ListItemExpandButton;
