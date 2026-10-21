import {fireEvent, render, screen, stubMutation, userEvent, waitFor} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppInstanceConfigurationDialog from '../components/AppInstanceConfigurationDialog';

const hoisted = vi.hoisted(() => ({
    configuration: {country: 'HR'} as unknown,
}));

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppInstanceConfigurationQuery: vi.fn(() => ({
        data: {appInstance: {configuration: hoisted.configuration, id: '11'}},
        isLoading: false,
    })),
    useUpdateAppInstanceConfigurationMutation: vi.fn(),
}));

const {useAppInstanceConfigurationQuery, useUpdateAppInstanceConfigurationMutation} =
    await import('@/shared/middleware/graphql');

const updateConfigurationMutation = stubMutation(vi.mocked(useUpdateAppInstanceConfigurationMutation));

const appInstance = {id: '11', name: 'crm-dev'};

const replaceConfigurationText = (text: string) => {
    fireEvent.change(screen.getByLabelText('Configuration (JSON)'), {target: {value: text}});
};

describe('AppInstanceConfigurationDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.configuration = {country: 'HR'};
    });

    it('shows the stored configuration as formatted JSON', async () => {
        render(<AppInstanceConfigurationDialog appInstance={appInstance} onOpenChange={vi.fn()} open={true} />);

        await waitFor(() => {
            expect(screen.getByLabelText('Configuration (JSON)')).toHaveValue(JSON.stringify({country: 'HR'}, null, 4));
        });

        expect(vi.mocked(useAppInstanceConfigurationQuery)).toHaveBeenCalledWith(
            {id: '11'},
            expect.objectContaining({enabled: true})
        );
    });

    it('saves a JSON object as the configuration', async () => {
        render(<AppInstanceConfigurationDialog appInstance={appInstance} onOpenChange={vi.fn()} open={true} />);

        replaceConfigurationText('{"country": "DE", "seats": 5}');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateConfigurationMutation.mutate).toHaveBeenCalledWith({
                configuration: {country: 'DE', seats: 5},
                id: '11',
            });
        });
    });

    it('refuses text that is not valid JSON', async () => {
        render(<AppInstanceConfigurationDialog appInstance={appInstance} onOpenChange={vi.fn()} open={true} />);

        replaceConfigurationText('{"country": ');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Configuration must be valid JSON')).toBeInTheDocument();
        expect(updateConfigurationMutation.mutate).not.toHaveBeenCalled();
    });

    it('refuses JSON that is not an object', async () => {
        render(<AppInstanceConfigurationDialog appInstance={appInstance} onOpenChange={vi.fn()} open={true} />);

        replaceConfigurationText('["country"]');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Configuration must be a JSON object')).toBeInTheDocument();
        expect(updateConfigurationMutation.mutate).not.toHaveBeenCalled();
    });

    it('refuses a configuration larger than 64 KB', async () => {
        render(<AppInstanceConfigurationDialog appInstance={appInstance} onOpenChange={vi.fn()} open={true} />);

        replaceConfigurationText(JSON.stringify({notes: 'a'.repeat(64 * 1024)}));

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Configuration must be at most 64 KB')).toBeInTheDocument();
        expect(updateConfigurationMutation.mutate).not.toHaveBeenCalled();
    });

    it('closes after a successful save', async () => {
        const onOpenChange = vi.fn();

        render(<AppInstanceConfigurationDialog appInstance={appInstance} onOpenChange={onOpenChange} open={true} />);

        replaceConfigurationText('{}');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateConfigurationMutation.mutate).toHaveBeenCalledWith({configuration: {}, id: '11'});
        });

        updateConfigurationMutation.triggerOnSuccess(undefined, {configuration: {}, id: '11'});

        await waitFor(() => {
            expect(onOpenChange).toHaveBeenCalledWith(false);
        });
    });
});
