import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import StorageUsageBanner from './StorageUsageBanner';

describe('StorageUsageBanner', () => {
    it('renders when usage is at or above 80 percent', () => {
        render(
            <StorageUsageBanner
                label="Data table"
                limitBytes={52_428_800}
                percentage={87}
                unlimited={false}
                usedBytes={45_613_056}
            />
        );

        expect(screen.getByRole('alert')).toBeInTheDocument();
        expect(screen.getByText(/87%/)).toBeInTheDocument();
    });

    it('renders nothing below 80 percent', () => {
        const {container} = render(
            <StorageUsageBanner
                label="Data table"
                limitBytes={52_428_800}
                percentage={50}
                unlimited={false}
                usedBytes={26_214_400}
            />
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing when unlimited', () => {
        const {container} = render(
            <StorageUsageBanner
                label="Data table"
                limitBytes={0}
                percentage={0}
                unlimited={true}
                usedBytes={99_999_999}
            />
        );

        expect(container).toBeEmptyDOMElement();
    });
});
