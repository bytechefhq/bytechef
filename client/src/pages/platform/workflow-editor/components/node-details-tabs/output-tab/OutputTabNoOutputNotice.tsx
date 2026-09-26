import Button from '@/components/Button/Button';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {CircleCheckIcon, XIcon} from 'lucide-react';

interface OutputTabNoOutputNoticeProps {
    className?: string;
    onDismiss: () => void;
    operationLabel: string;
}

const OutputTabNoOutputNotice = ({className, onDismiss, operationLabel}: OutputTabNoOutputNoticeProps) => (
    <Alert className={className} variant="success">
        <CircleCheckIcon />

        <AlertTitle className="pr-6">Test completed</AlertTitle>

        <AlertDescription className="col-span-2 col-start-1 mt-1">
            {`The ${operationLabel.toLowerCase()} ran successfully but returned no data.`}
        </AlertDescription>

        <Button
            aria-label="Dismiss notice"
            className="absolute top-2 right-2"
            icon={<XIcon />}
            onClick={onDismiss}
            size="iconXs"
            variant="ghost"
        />
    </Alert>
);

export default OutputTabNoOutputNotice;
