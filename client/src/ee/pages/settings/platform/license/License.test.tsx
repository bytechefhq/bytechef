import {createTestQueryClientWrapper} from '@/shared/util/test-utils';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import License from './License';

// ---------------------------------------------------------------------------
// Hoisted mocks (must not reference outer-scope constants — vi.hoisted runs
// before module initialisation)
// ---------------------------------------------------------------------------

const {licenceQueryMock} = vi.hoisted(() => ({
    licenceQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteLicenceMutation: () => ({mutate: vi.fn()}),
    useLicenceQuery: licenceQueryMock,
    useUploadLicenceMutation: () => ({mutateAsync: vi.fn()}),
}));

const renderWithProviders = (ui: ReactNode) => {
    const QueryClientWrapper = createTestQueryClientWrapper();

    return render(<QueryClientWrapper>{ui}</QueryClientWrapper>);
};

describe('License', () => {
    it('renders the upload UI when there is no licence (status MISSING)', () => {
        licenceQueryMock.mockReturnValue({
            data: {licence: null},
            error: null,
            isLoading: false,
        });

        renderWithProviders(<License />);

        expect(screen.getByText('License File')).toBeInTheDocument();
        expect(screen.getByText(/drop your license file here/i)).toBeInTheDocument();
    });

    it('renders the upload UI when licence status is MISSING (non-null but missing)', () => {
        licenceQueryMock.mockReturnValue({
            data: {licence: {status: 'MISSING'}},
            error: null,
            isLoading: false,
        });

        renderWithProviders(<License />);

        expect(screen.getByText('License File')).toBeInTheDocument();
    });

    it('renders licence details when a VALID licence is present', () => {
        licenceQueryMock.mockReturnValue({
            data: {
                licence: {
                    allowedJobs: 1000,
                    currentMonthJobUsage: 42,
                    expiresAt: '2027-01-01T00:00:00Z',
                    features: ['AI', 'SSO'],
                    holderEmail: 'admin@example.com',
                    holderName: 'Acme Corp',
                    id: 'lic-1',
                    issuedAt: '2026-01-01T00:00:00Z',
                    maxUsers: 50,
                    status: 'VALID',
                },
            },
            error: null,
            isLoading: false,
        });

        renderWithProviders(<License />);

        expect(screen.getByText('Acme Corp')).toBeInTheDocument();
        expect(screen.getByText('admin@example.com')).toBeInTheDocument();
        expect(screen.getByText('Valid')).toBeInTheDocument();
        expect(screen.getByText('AI')).toBeInTheDocument();
        expect(screen.getByText('SSO')).toBeInTheDocument();
        expect(screen.getByText('1000')).toBeInTheDocument();
        expect(screen.getByText('42')).toBeInTheDocument();
    });
});
