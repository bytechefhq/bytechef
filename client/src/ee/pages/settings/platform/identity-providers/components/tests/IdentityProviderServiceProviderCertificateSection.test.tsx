import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IdentityProviderServiceProviderCertificateSection from '../IdentityProviderServiceProviderCertificateSection';

type RegenerateDataType = {
    regenerateIdentityProviderServiceProviderCertificate: {id: string; serviceProviderCertificate: string | null};
};

const hoisted = vi.hoisted(() => ({
    mutate: vi.fn(),
    onSuccess: undefined as ((data: RegenerateDataType) => void) | undefined,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useRegenerateIdentityProviderServiceProviderCertificateMutation: (options: {
        onSuccess: (data: RegenerateDataType) => void;
    }) => {
        hoisted.onSuccess = options.onSuccess;

        return {isPending: false, mutate: hoisted.mutate};
    },
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

describe('IdentityProviderServiceProviderCertificateSection', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.mutate.mockImplementation(() => {
            hoisted.onSuccess?.({
                regenerateIdentityProviderServiceProviderCertificate: {
                    id: '7',
                    serviceProviderCertificate: 'NEW CERTIFICATE',
                },
            });
        });
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('shows the current certificate', () => {
        render(
            <IdentityProviderServiceProviderCertificateSection
                identityProviderId="7"
                serviceProviderCertificate="CURRENT CERTIFICATE"
            />
        );

        expect(screen.getByLabelText('SP signing certificate')).toHaveValue('CURRENT CERTIFICATE');
    });

    it('explains when the certificate has not been created yet', () => {
        render(
            <IdentityProviderServiceProviderCertificateSection
                identityProviderId="7"
                serviceProviderCertificate={null}
            />
        );

        expect(screen.queryByLabelText('SP signing certificate')).not.toBeInTheDocument();
        expect(screen.getByText(/created on the first sign-in/i)).toBeInTheDocument();
    });

    it('regenerates only after confirmation and shows the new certificate', async () => {
        render(
            <IdentityProviderServiceProviderCertificateSection
                identityProviderId="7"
                serviceProviderCertificate="CURRENT CERTIFICATE"
            />
        );

        await userEvent.click(screen.getByRole('button', {name: /regenerate certificate/i}));

        expect(hoisted.mutate).not.toHaveBeenCalled();
        expect(screen.getByRole('alertdialog')).toHaveTextContent(/reject sign-ins until you register/i);

        await userEvent.click(screen.getByRole('button', {name: 'Regenerate'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({id: '7'});
        expect(screen.getByLabelText('SP signing certificate')).toHaveValue('NEW CERTIFICATE');
    });

    it('keeps the certificate when the confirmation is cancelled', async () => {
        render(
            <IdentityProviderServiceProviderCertificateSection
                identityProviderId="7"
                serviceProviderCertificate="CURRENT CERTIFICATE"
            />
        );

        await userEvent.click(screen.getByRole('button', {name: /regenerate certificate/i}));
        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(hoisted.mutate).not.toHaveBeenCalled();
        expect(screen.getByLabelText('SP signing certificate')).toHaveValue('CURRENT CERTIFICATE');
    });
});
