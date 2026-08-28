import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import DataTableListItemTooltip from '../DataTableListItemTooltip';

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

const renderWithTooltipProvider = (lastModifiedDate?: number | null) => {
    return render(
        <TooltipProvider>
            <DataTableListItemTooltip lastModifiedDate={lastModifiedDate} />
        </TooltipProvider>
    );
};

describe('DataTableListItemTooltip', () => {
    it('should render last modified text', () => {
        renderWithTooltipProvider(1705000000000);

        expect(screen.getByText(/Last modified:/)).toBeInTheDocument();
    });

    it('should display formatted date when lastModifiedDate is provided', () => {
        const timestamp = new Date('2024-01-11T12:00:00').getTime();

        renderWithTooltipProvider(timestamp);

        const formattedDate = new Date(timestamp).toLocaleString();

        expect(screen.getByText(`Last modified: ${formattedDate}`)).toBeInTheDocument();
    });

    it('should display N/A when lastModifiedDate is null', () => {
        renderWithTooltipProvider(null);

        expect(screen.getByText('Last modified: N/A')).toBeInTheDocument();
    });

    it('should display N/A when lastModifiedDate is undefined', () => {
        renderWithTooltipProvider(undefined);

        expect(screen.getByText('Last modified: N/A')).toBeInTheDocument();
    });
});
