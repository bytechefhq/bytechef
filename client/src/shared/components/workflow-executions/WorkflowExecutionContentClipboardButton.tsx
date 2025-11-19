import Button from '@/components/Button/Button';
import {useToast} from '@/hooks/use-toast';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {CheckIcon, ClipboardCopyIcon} from 'lucide-react';
import {useEffect} from 'react';

const SPACE = 4;

/* eslint-disable  @typescript-eslint/no-explicit-any */
const WorkflowExecutionContentClipboardButton = ({value}: {value: any}) => {
    const [copiedText, setCopiedText] = useCopyToClipboard();

    const hasCopiedText = Boolean(copiedText);
    const {toast} = useToast();

    useEffect(() => {
        if (hasCopiedText) {
            toast({
                description: 'The value has been copied to your clipboard.',
                title: 'Copied to clipboard',
            });
        }
    }, [hasCopiedText, toast]);

    return (
        value &&
        (typeof value !== 'object' || Object.keys(value).length > 0) && (
            <Button
                disabled={hasCopiedText}
                icon={hasCopiedText ? <CheckIcon className="text-success" /> : <ClipboardCopyIcon />}
                onClick={() => {
                    setCopiedText(typeof value === 'object' ? JSON.stringify(value, null, SPACE) : value);
                }}
                size="iconXs"
                variant="ghost"
            />
        )
    );
};

export default WorkflowExecutionContentClipboardButton;
