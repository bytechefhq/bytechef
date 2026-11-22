import LoadingDots from '@/components/LoadingDots';
import {Suspense, lazy} from 'react';

import type {ComponentProps} from 'react';

const CopilotPanelImpl = lazy(() => import('@/shared/components/copilot/CopilotPanelImpl'));

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
