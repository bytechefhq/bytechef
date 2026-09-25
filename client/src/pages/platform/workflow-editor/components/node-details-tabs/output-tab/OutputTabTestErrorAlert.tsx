import Button from '@/components/Button/Button';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {CircleXIcon, XIcon} from 'lucide-react';

import {TestOutputErrorI} from './hooks/useOutputTab';

interface OutputTabTestErrorAlertProps {
    className?: string;
    onDismiss: () => void;
    testOutputError: TestOutputErrorI;
}

const OutputTabTestErrorAlert = ({className, onDismiss, testOutputError}: OutputTabTestErrorAlertProps) => (
    <Alert className={className} variant="destructive">
        <CircleXIcon />

        <AlertTitle className="pr-6">{testOutputError.title}</AlertTitle>

        <AlertDescription className="col-span-2 col-start-1 mt-1 max-h-48 min-w-0 overflow-y-auto font-mono text-xs wrap-anywhere whitespace-pre-wrap">
            {testOutputError.message}
        </AlertDescription>

        <Button
            aria-label="Dismiss error"
            className="absolute top-2 right-2"
            icon={<XIcon />}
            onClick={onDismiss}
            size="iconXs"
            variant="ghost"
        />
    </Alert>
);

export default OutputTabTestErrorAlert;
