import {ScrollArea} from '@/components/ui/scroll-area';
import {render} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

describe('ScrollArea', () => {
    it('passes the root max-height down to the scrolling viewport', () => {
        const {container} = render(
            <ScrollArea className="max-h-[132px]">
                <ul>
                    <li>an item</li>
                </ul>
            </ScrollArea>
        );

        const viewport = container.querySelector('[data-slot="scroll-area-viewport"]');

        expect(viewport).not.toBeNull();
        expect(viewport).toHaveClass('max-h-[inherit]');
    });

    it('keeps the caller class on the root', () => {
        const {container} = render(
            <ScrollArea className="max-h-[132px]">
                <span>content</span>
            </ScrollArea>
        );

        expect(container.querySelector('[data-slot="scroll-area"]')).toHaveClass('max-h-[132px]');
    });
});
