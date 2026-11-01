import {render, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import AiHubSettings from '../AiHubSettings';

vi.mock('@/ee/pages/automation/ai-hub/settings/ToolApprovals', () => ({
    default: () => <section aria-label="Tool Approvals" />,
}));

describe('AiHubSettings', () => {
    it('titles the page AI Hub and shows Tool Approvals as a section on it', () => {
        render(
            <MemoryRouter>
                <AiHubSettings />
            </MemoryRouter>
        );

        expect(screen.getByText('AI Hub')).toBeInTheDocument();
        expect(screen.getByRole('region', {name: 'Tool Approvals'})).toBeInTheDocument();
    });
});
