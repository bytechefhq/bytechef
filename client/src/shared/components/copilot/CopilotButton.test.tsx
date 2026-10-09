import {TooltipProvider} from '@/components/ui/tooltip';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import CopilotButton from './CopilotButton';

const hoisted = vi.hoisted(() => ({
    copilotEnabled: true,
    enabledFeatureFlags: ['ff-1570'] as string[],
    openCopilot: vi.fn(),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {ai: {copilot: {enabled: boolean}}}) => unknown) =>
        selector({ai: {copilot: {enabled: hoisted.copilotEnabled}}}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => (featureFlag: string) => hoisted.enabledFeatureFlags.includes(featureFlag),
}));

vi.mock('@/shared/components/copilot/hooks/useOpenCopilot', () => ({
    default: () => hoisted.openCopilot,
}));

const renderCopilotButton = (parameters?: Record<string, unknown>) =>
    render(
        <TooltipProvider>
            <CopilotButton parameters={parameters} source={Source.MCP_SERVER} />
        </TooltipProvider>
    );

describe('CopilotButton', () => {
    beforeEach(() => {
        hoisted.copilotEnabled = true;
        hoisted.enabledFeatureFlags = ['ff-1570'];
        hoisted.openCopilot.mockReset();
    });

    it('should open the copilot for its source and parameters when clicked', async () => {
        renderCopilotButton({mcpServerId: 3});

        await userEvent.click(screen.getByRole('button', {name: 'Ask Copilot'}));

        expect(hoisted.openCopilot).toHaveBeenCalledWith({parameters: {mcpServerId: 3}, source: Source.MCP_SERVER});
    });

    it('should open the copilot with empty parameters by default', async () => {
        renderCopilotButton();

        await userEvent.click(screen.getByRole('button', {name: 'Ask Copilot'}));

        expect(hoisted.openCopilot).toHaveBeenCalledWith({parameters: {}, source: Source.MCP_SERVER});
    });

    it('should render nothing when the copilot is disabled', () => {
        hoisted.copilotEnabled = false;

        renderCopilotButton();

        expect(screen.queryByRole('button', {name: 'Ask Copilot'})).not.toBeInTheDocument();
    });

    it('should render nothing when the copilot feature flag is off', () => {
        hoisted.enabledFeatureFlags = [];

        renderCopilotButton();

        expect(screen.queryByRole('button', {name: 'Ask Copilot'})).not.toBeInTheDocument();
    });
});
