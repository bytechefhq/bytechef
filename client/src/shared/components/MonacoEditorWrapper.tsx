import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {Suspense, lazy} from 'react';

import type {ComponentProps} from 'react';

const MonacoEditorWrapperImpl = lazy(() => import('@/shared/components/MonacoEditorWrapperImpl'));

/**
 * Lazy boundary for Monaco. The implementation (and monaco-editor + its workers)
 * loads only when an editor actually renders, keeping monaco out of every
 * initial chunk. All import sites keep using this path unchanged.
 */
const MonacoEditorWrapper = (props: ComponentProps<typeof MonacoEditorWrapperImpl>) => (
    <Suspense fallback={<MonacoEditorLoader />}>
        <MonacoEditorWrapperImpl {...props} />
    </Suspense>
);

export default MonacoEditorWrapper;
