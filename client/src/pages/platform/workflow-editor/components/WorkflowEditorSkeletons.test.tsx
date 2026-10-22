import {render} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {OutputTabSkeleton} from './WorkflowEditorSkeletons';

describe('OutputTabSkeleton', () => {
    it('renders a heading and a body placeholder', () => {
        const {container} = render(<OutputTabSkeleton />);

        expect(container.querySelectorAll('[data-slot="skeleton"]')).toHaveLength(2);
    });
});
