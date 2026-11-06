import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {SquareArrowOutUpRightIcon} from 'lucide-react';
import {MouseEvent} from 'react';

interface OpenInButtonProps {
    label: string;
    onClick: () => void;
}

const OpenInButton = ({label, onClick}: OpenInButtonProps) => {
    const handleClick = (event: MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();

        onClick();
    };

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    aria-label={label}
                    className="-ml-1.5 size-5 shrink-0 [&_svg]:size-3"
                    icon={<SquareArrowOutUpRightIcon className="size-3" />}
                    onClick={handleClick}
                    size="icon"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>{label}</TooltipContent>
        </Tooltip>
    );
};

export default OpenInButton;
