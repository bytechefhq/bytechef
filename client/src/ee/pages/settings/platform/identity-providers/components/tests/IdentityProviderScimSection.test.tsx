import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IdentityProviderScimSection from '../IdentityProviderScimSection';

const hoisted = vi.hoisted(() => ({
    mutate: vi.fn(),
    onSuccess: undefined as ((data: {generateIdentityProviderScimApiKey: string}) => void) | undefined,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useGenerateIdentityProviderScimApiKeyMutation: (options: {
        onSuccess: (data: {generateIdentityProviderScimApiKey: string}) => void;
    }) => {
        hoisted.onSuccess = options.onSuccess;

        return {isPending: false, mutate: hoisted.mutate};
    },
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

describe('IdentityProviderScimSection', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.mutate.mockImplementation(() => {
            hoisted.onSuccess?.({generateIdentityProviderScimApiKey: 'bcscim_public_secret'});
        });
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('shows the SCIM base URL', () => {
        render(<IdentityProviderScimSection identityProviderId="7" scimEnabled={false} />);

        expect(screen.getByLabelText('SCIM base URL')).toHaveValue(`${window.location.origin}/api/scim/v2`);
    });

    it('shows a generated token once and offers rotation afterwards', async () => {
        render(<IdentityProviderScimSection identityProviderId="7" scimEnabled={false} />);

        expect(screen.queryByLabelText('SCIM token')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: /generate scim token/i}));

        expect(hoisted.mutate).toHaveBeenCalledWith({id: '7'});
        expect(screen.getByLabelText('SCIM token')).toHaveValue('bcscim_public_secret');
        expect(screen.getByRole('button', {name: /rotate scim token/i})).toBeInTheDocument();
    });

    it('offers rotation when a token already exists', () => {
        render(<IdentityProviderScimSection identityProviderId="7" scimEnabled={true} />);

        expect(screen.getByRole('button', {name: /rotate scim token/i})).toBeInTheDocument();
    });
});
