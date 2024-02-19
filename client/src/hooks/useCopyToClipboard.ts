import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import {useState} from 'react';

type CopiedValue = string | null;
type CopyFn = (text: string) => Promise<boolean>; // Return success

function useCopyToClipboard(): [CopiedValue, CopyFn] {
    const [copiedText, setCopiedText] = useState<CopiedValue>(null);

    const {setCopiedPropertyData} = useWorkflowNodeDetailsPanelStore();

    const copy: CopyFn = async (text) => {
        if (!navigator?.clipboard) {
            console.warn('Clipboard not supported');
            return false;
        }

        // Try to save to clipboard then save it in the state if worked
        try {
            await navigator.clipboard.writeText(text);

            setCopiedText(text);

            const extractedValue = text.replace(/\${|}/g, '');

            const nodeName = extractedValue.split('.')[0];

            setCopiedPropertyData({
                componentIcon: '📄',
                id: nodeName,
                value: extractedValue,
            });

            return true;
        } catch (error) {
            console.warn('Copy failed', error);

            setCopiedText(null);

            return false;
        }
    };

    return [copiedText, copy];
}

export default useCopyToClipboard;
