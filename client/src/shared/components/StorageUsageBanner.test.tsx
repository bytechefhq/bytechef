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

    it('shows sizes below one megabyte in smaller units', () => {
        render(
            <StorageUsageBanner
                label="Knowledge base"
                limitBytes={500_000}
                percentage={80}
                unlimited={false}
                usedBytes={400_000}
            />
        );

        expect(screen.getByText(/Using 391 KB of 488 KB/)).toBeInTheDocument();
    });

    it('shows sizes of one gigabyte and more in gigabytes', () => {
        render(
            <StorageUsageBanner
                label="Knowledge base"
                limitBytes={1_073_741_824}
                percentage={95}
                unlimited={false}
                usedBytes={1_020_054_733}
            />
        );

        expect(screen.getByText(/Using 973 MB of 1.0 GB/)).toBeInTheDocument();
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
