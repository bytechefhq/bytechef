import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import CopilotPanel from './CopilotPanel';

// Keeps the lazy chunk permanently pending so the Suspense fallback is what gets rendered. The
// panel is mounted unconditionally by the app shell, so this is the state every page load passes
// through while the copilot chunk is still in flight.
vi.mock('@/shared/components/copilot/CopilotPanelImpl', () => new Promise(() => {}));

describe('CopilotPanel', () => {
    it('renders no loading indicator while the chunk loads and the panel is closed', () => {
        const {container} = render(<CopilotPanel open={false} />);

        expect(container.querySelector('.animate-pulse')).toBeNull();
    });

    it('renders a loading indicator while the chunk loads and the panel is open', async () => {
        const {container} = render(<CopilotPanel open={true} />);

        await vi.waitFor(() => expect(container.querySelector('.animate-pulse')).not.toBeNull());

        expect(screen.queryByRole('alert')).toBeNull();
    });
});
