import {render, resetAll, screen, within} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it} from 'vitest';

import ConnectedUserSheetPanelListSkeleton, {
    ConnectedUserSheetPanelListSkeletonVariantType,
} from '../ConnectedUserSheetPanelListSkeleton';

const SKELETON_PARTS = [
    'toolbar',
    'expand',
    'groupHeader',
    'icon',
    'version',
    'switch',
    'badges',
    'date',
    'menu',
] as const;

const EXPECTED_PARTS: Record<ConnectedUserSheetPanelListSkeletonVariantType, (typeof SKELETON_PARTS)[number][]> = {
    automation: ['toolbar', 'expand', 'groupHeader', 'version', 'date', 'menu'],
    connection: ['icon', 'badges', 'date', 'menu'],
    integration: ['toolbar', 'expand', 'icon', 'version', 'switch', 'date', 'menu'],
    mcpServer: ['toolbar', 'expand', 'switch', 'date', 'menu'],
};

describe('ConnectedUserSheetPanelListSkeleton', () => {
    afterEach(() => {
        resetAll();
    });

    it.each(Object.keys(EXPECTED_PARTS) as ConnectedUserSheetPanelListSkeletonVariantType[])(
        'renders only the parts a real %s row has',
        (variant) => {
            render(<ConnectedUserSheetPanelListSkeleton label="Loading" rowCount={1} variant={variant} />);

            const skeleton = screen.getByRole('status', {name: 'Loading'});

            for (const part of SKELETON_PARTS) {
                const partElements = within(skeleton).queryAllByTestId(`skeleton-${part}`);

                expect(partElements, `${variant} ${part}`).toHaveLength(EXPECTED_PARTS[variant].includes(part) ? 1 : 0);
            }
        }
    );

    it('renders the requested number of rows', () => {
        render(<ConnectedUserSheetPanelListSkeleton label="Loading" rowCount={2} variant="connection" />);

        expect(screen.getAllByTestId('skeleton-menu')).toHaveLength(2);
    });

    it('renders one toolbar above all rows', () => {
        render(<ConnectedUserSheetPanelListSkeleton label="Loading" rowCount={3} variant="integration" />);

        expect(screen.getAllByTestId('skeleton-toolbar')).toHaveLength(1);
    });
});
