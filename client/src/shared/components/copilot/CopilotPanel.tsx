import LoadingDots from '@/components/LoadingDots';
import {Suspense, lazy} from 'react';

import type {ComponentProps} from 'react';

const CopilotPanelImpl = lazy(() => import('@/shared/components/copilot/CopilotPanelImpl'));

/**
 * Lazy boundary for the copilot panel: @assistant-ui/react and the AG-UI client
 * load on first open instead of riding in the editor's initial chunk. Mount
 * sites already render this conditionally, so the chunk fetch happens on open.
 */
const CopilotPanel = (props: ComponentProps<typeof CopilotPanelImpl>) => (
    <Suspense
        fallback={
            <div className="flex size-full items-center justify-center p-4">
                <LoadingDots />
            </div>
        }
    >
        <CopilotPanelImpl {...props} />
    </Suspense>
);

export default CopilotPanel;
