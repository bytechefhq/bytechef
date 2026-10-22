import Button from '@/components/Button/Button';

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
}: OutputTabTestButtonProps) => (
    <Button
        className={className}
        disabled={connectionMissing || missingRequiredProperties.length > 0 || pending}
        label={label}
        onClick={onClick}
        type="button"
        variant={variant}
    />
);

export default OutputTabTestButton;
