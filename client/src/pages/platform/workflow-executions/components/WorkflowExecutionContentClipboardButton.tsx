import {useCopyToClipboard} from '@uidotdev/usehooks';
import {ClipboardCopyIcon} from 'lucide-react';

const SPACE = 4;

/* eslint-disable  @typescript-eslint/no-explicit-any */
const WorkflowExecutionContentClipboardButton = ({value}: {value: any}) => {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, setCopiedText] = useCopyToClipboard();

    return (
        value &&
        (typeof value !== 'object' || Object.keys(value).length > 0) && (
            <ClipboardCopyIcon
                className="h-4 cursor-pointer"
                onClick={() => setCopiedText(typeof value === 'object' ? JSON.stringify(value, null, SPACE) : value)}
            />
        )
    );
};

export default WorkflowExecutionContentClipboardButton;
