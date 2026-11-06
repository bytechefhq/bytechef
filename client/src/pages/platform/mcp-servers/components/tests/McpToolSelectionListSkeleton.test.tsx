import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import McpToolSelectionListSkeleton from '../McpToolSelectionListSkeleton';

describe('McpToolSelectionListSkeleton', () => {
    it('should render a busy tool list without a heading by default', () => {
        render(<McpToolSelectionListSkeleton />);

        expect(screen.getByRole('status', {name: 'Loading tools'})).toHaveAttribute('aria-busy', 'true');
        expect(screen.queryByTestId('mcp-tools-heading-skeleton')).not.toBeInTheDocument();
    });

    it('should render a heading placeholder when requested', () => {
        render(<McpToolSelectionListSkeleton showHeading />);

        expect(screen.getByTestId('mcp-tools-heading-skeleton')).toBeInTheDocument();
    });
});
