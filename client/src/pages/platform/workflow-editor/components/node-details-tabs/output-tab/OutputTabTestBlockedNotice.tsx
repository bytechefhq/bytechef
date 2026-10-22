import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {TriangleAlertIcon} from 'lucide-react';

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
        return (
            <Alert className={className} variant="warning">
                <TriangleAlertIcon />

                <AlertTitle>Required properties missing</AlertTitle>

                <AlertDescription>
                    <p>{`Set these properties to test the ${operationText}:`}</p>

                    <ul className="list-disc pl-4">
                        {missingRequiredProperties.map((missingRequiredProperty) => (
                            <li key={missingRequiredProperty}>
                                {onMissingRequiredPropertiesClick ? (
                                    <button
                                        className="font-medium underline-offset-2 hover:underline"
                                        onClick={onMissingRequiredPropertiesClick}
                                        type="button"
                                    >
                                        {missingRequiredProperty}
                                    </button>
                                ) : (
                                    <span className="font-medium">{missingRequiredProperty}</span>
                                )}
                            </li>
                        ))}
                    </ul>
                </AlertDescription>
            </Alert>
        );
    }

    if (connectionMissing) {
        return (
            <Alert className={className} variant="warning">
                <TriangleAlertIcon />

                <AlertTitle>Connection missing</AlertTitle>

                <AlertDescription>{`Select a connection to test the ${operationText}.`}</AlertDescription>
            </Alert>
        );
    }

    return null;
};

export default OutputTabTestBlockedNotice;
