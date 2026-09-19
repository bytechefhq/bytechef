import {TooltipProvider} from '@/components/ui/tooltip';
import {render} from '@/shared/util/test-utils';
import {fireEvent} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import PropertyField from './PropertyField';

describe('PropertyField', () => {
    it('should copy an array item path with index 0 instead of the [index] placeholder', () => {
        const copyToClipboard = vi.fn().mockResolvedValue(undefined);

        const {container} = render(
            <TooltipProvider>
                <PropertyField
                    copiedValue={null}
                    copyToClipboard={copyToClipboard}
                    label="itemUrl"
                    parentPath="data.json.result[index]"
                    property={{name: 'itemUrl', type: 'STRING'}}
                    sampleOutput={{}}
                    workflowNodeName="firecrawl_1"
                />
            </TooltipProvider>
        );

        fireEvent.click(container.querySelector('.lucide-clipboard')!);

        expect(copyToClipboard).toHaveBeenCalledWith('${firecrawl_1.data.json.result[0].itemUrl}');
    });

    it('should copy the path of an unnamed array item with index 0', () => {
        const copyToClipboard = vi.fn().mockResolvedValue(undefined);

        const {container} = render(
            <TooltipProvider>
                <PropertyField
                    copiedValue={null}
                    copyToClipboard={copyToClipboard}
                    label="[index]"
                    parentPath="data.items"
                    property={{type: 'STRING'}}
                    sampleOutput={{}}
                    workflowNodeName="script_1"
                />
            </TooltipProvider>
        );

        fireEvent.click(container.querySelector('.lucide-clipboard')!);

        expect(copyToClipboard).toHaveBeenCalledWith('${script_1.data.items[0]}');
    });
});
