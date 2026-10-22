import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {CircleAlertIcon} from 'lucide-react';

interface OutputTabTestBlockedNoticeProps {
    className?: string;
    connectionMissing: boolean;
    missingRequiredProperties: string[];
    onMissingRequiredPropertiesClick?: () => void;
    operationLabel: string;
}

const OutputTabTestBlockedNotice = ({
    className,
    connectionMissing,
    missingRequiredProperties,
    onMissingRequiredPropertiesClick,
    operationLabel,
}: OutputTabTestBlockedNoticeProps) => {
    const operationText = operationLabel.toLowerCase();

    if (missingRequiredProperties.length) {
        const missingRequiredPropertiesText = missingRequiredProperties.join(', ');

        return (
            <Alert className={className}>
                <CircleAlertIcon />

                <AlertTitle>Required properties missing</AlertTitle>

                <AlertDescription>
                    <p>
                        <span>Set </span>

                        {onMissingRequiredPropertiesClick ? (
                            <button
                                className="font-medium text-content-brand-primary underline-offset-2 hover:underline"
                                onClick={onMissingRequiredPropertiesClick}
                                type="button"
                            >
                                {missingRequiredPropertiesText}
                            </button>
                        ) : (
                            <span className="font-medium">{missingRequiredPropertiesText}</span>
                        )}

                        <span>{` to test the ${operationText}.`}</span>
                    </p>
                </AlertDescription>
            </Alert>
        );
    }

    if (connectionMissing) {
        return (
            <Alert className={className}>
                <CircleAlertIcon />

                <AlertTitle>Connection missing</AlertTitle>

                <AlertDescription>{`Select a connection to test the ${operationText}.`}</AlertDescription>
            </Alert>
        );
    }

    return null;
};

export default OutputTabTestBlockedNotice;
