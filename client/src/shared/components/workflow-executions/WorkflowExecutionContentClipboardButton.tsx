import Button from '@/components/Button/Button';
import {useToast} from '@/hooks/use-toast';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {CheckIcon, ClipboardCopyIcon} from 'lucide-react';
import {useEffect, useState} from 'react';

const SPACE = 4;
const RESET_DELAY = 2000;

/* eslint-disable  @typescript-eslint/no-explicit-any */
const WorkflowExecutionContentClipboardButton = ({value}: {value: any}) => {
    const [lastCopiedValue, setLastCopiedValue] = useState<string | null>(null);

    const [, copyToClipboard] = useCopyToClipboard();
    const {toast} = useToast();

    const valueToCopy = typeof value === 'object' ? JSON.stringify(value, null, SPACE) : value;
    const isCurrentlyCopied = lastCopiedValue === valueToCopy;

    const handleCopyText = async () => {
        try {
            await copyToClipboard(valueToCopy);

            setLastCopiedValue(valueToCopy);
        } catch {
            toast({
                description: 'Failed to copy to clipboard. Please try again.',
                title: 'Copy failed',
                variant: 'destructive',
            });
        }
    };

    useEffect(() => {
        if (lastCopiedValue !== null) {
            toast({
                description: 'The value has been copied to your clipboard.',
                title: 'Copied to clipboard',
            });

            const timer = setTimeout(() => {
                setLastCopiedValue(null);
            }, RESET_DELAY);

            return () => clearTimeout(timer);
        }
    }, [lastCopiedValue, toast]);

    return (
        value &&
        (typeof value !== 'object' || Object.keys(value).length > 0) && (
            <Button
                disabled={isCurrentlyCopied}
                icon={isCurrentlyCopied ? <CheckIcon className="text-success" /> : <ClipboardCopyIcon />}
                onClick={handleCopyText}
                size="iconXs"
                variant="ghost"
            />
        )
    );
};

export default WorkflowExecutionContentClipboardButton;
