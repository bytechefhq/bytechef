import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {InfoIcon} from 'lucide-react';

interface OutputTabTruncatedOutputNoticeProps {
    className?: string;
}

const OutputTabTruncatedOutputNotice = ({className}: OutputTabTruncatedOutputNoticeProps) => (
    <Alert className={className}>
        <InfoIcon />

        <AlertTitle>Output shortened</AlertTitle>

        <AlertDescription>
            Long lists in this test output were cut to their first items. Workflow runs use the full output.
        </AlertDescription>
    </Alert>
);

export default OutputTabTruncatedOutputNotice;
