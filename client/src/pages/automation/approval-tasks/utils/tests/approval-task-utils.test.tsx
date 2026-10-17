import {render} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import {getPriorityColor, getStatusIcon} from '../approval-task-utils';

describe('getPriorityColor', () => {
    it.each([
        ['high', 'bg-surface-destructive-secondary text-content-destructive border-stroke-destructive-secondary'],
        ['medium', 'bg-surface-warning-secondary text-content-warning-primary border-stroke-warning-secondary'],
        ['low', 'bg-surface-success-secondary text-content-success-primary border-stroke-success-secondary'],
        ['unknown', 'bg-surface-neutral-secondary text-content-neutral-primary border-stroke-neutral-secondary'],
    ])('colours %s priority', (priority, expectedClasses) => {
        expect(getPriorityColor(priority)).toBe(expectedClasses);
    });
});

describe('getStatusIcon', () => {
    it.each([
        ['completed', 'text-content-success-primary'],
        ['in-progress', 'text-content-brand-primary'],
        ['open', 'text-content-neutral-tertiary'],
    ])('colours the %s icon', (status, expectedClass) => {
        const {container} = render(<>{getStatusIcon(status)}</>);

        expect(container.querySelector('svg')).toHaveClass('size-4', expectedClass);
    });
});
