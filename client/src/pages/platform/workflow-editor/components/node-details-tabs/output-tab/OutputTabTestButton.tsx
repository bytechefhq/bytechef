import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useMemo} from 'react';

interface OutputTabTestButtonProps {
    className?: string;
    connectionMissing: boolean;
    label: string;
    missingRequiredProperties: string[];
    onClick: () => void;
    pending: boolean;
    variant?: 'outline';
}

const OutputTabTestButton = ({
    className,
    connectionMissing,
    label,
    missingRequiredProperties,
    onClick,
    pending,
    variant,
}: OutputTabTestButtonProps) => {
    const disabledReason = useMemo(() => {
        if (missingRequiredProperties.length) {
            return `Set the required properties first: ${missingRequiredProperties.join(', ')}`;
        }

        if (connectionMissing) {
            return 'Select a connection first';
        }

        return undefined;
    }, [connectionMissing, missingRequiredProperties]);

    const button = (
        <Button
            className={className}
            disabled={!!disabledReason || pending}
            label={label}
            onClick={onClick}
            type="button"
            variant={variant}
        />
    );

    if (!disabledReason) {
        return button;
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span tabIndex={0}>{button}</span>
            </TooltipTrigger>

            <TooltipContent className="max-w-xs">{disabledReason}</TooltipContent>
        </Tooltip>
    );
};

export default OutputTabTestButton;
