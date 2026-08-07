import LoadingDots from '@/components/LoadingDots';
import {Suspense, lazy} from 'react';

import type {ComponentProps} from 'react';

const CopilotPanelImpl = lazy(() => import('@/shared/components/copilot/CopilotPanelImpl'));

/**
 * Lazy boundary for the copilot panel: @assistant-ui/react and the AG-UI client
 * load in their own chunk instead of riding in the editor's initial chunk.
 *
 * The app shell mounts this on every authenticated page regardless of whether the panel is open, so
 * the chunk is fetched during the initial page load. The fallback is therefore gated on `open`: a
 * closed panel collapses to zero width once loaded, and showing a full-height loading indicator for
 * it would put a second spinner on the right edge of every page — and shift the main content left
 * while it is there.
 */
const CopilotPanel = (props: ComponentProps<typeof CopilotPanelImpl>) => (
    <Suspense
        fallback={
            props.open ? (
                <div className="flex size-full items-center justify-center p-4">
                    <LoadingDots />
                </div>
            ) : null
        }
    >
        <CopilotPanelImpl {...props} />
    </Suspense>
);

export default CopilotPanel;
