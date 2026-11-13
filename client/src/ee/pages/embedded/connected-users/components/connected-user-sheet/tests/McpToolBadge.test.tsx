import {TooltipProvider} from '@/components/ui/tooltip';
import McpToolBadge from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/McpToolBadge';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it} from 'vitest';

describe('McpToolBadge', () => {
    it('explains that the switch turns on the other triggers and the MCP Servers tab controls the tool', async () => {
        render(
            <TooltipProvider>
                <McpToolBadge />
            </TooltipProvider>
        );

        await userEvent.hover(screen.getByText('MCP tool'));

        expect(
            (
                await screen.findAllByText(
                    "An MCP server also exposes this workflow as a tool. This switch turns on the workflow's other triggers; whether the user's AI agents can call the tool is set on the MCP Servers tab."
                )
            ).length
        ).toBeGreaterThan(0);
    });
});
