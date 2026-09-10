import {AiProvider} from '@/shared/middleware/platform/configuration';
import {render, resetAll, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import AiProviderForm from './AiProviderForm';

const {mutateMock, onSuccessRef} = vi.hoisted(() => ({
    mutateMock: vi.fn(),
    onSuccessRef: {current: undefined as (() => void) | undefined},
}));

vi.mock('@/shared/mutations/platform/aiProvider.mutations', () => ({
    useUpdateAiProviderMutation: ({onSuccess}: {onSuccess?: () => void}) => {
        onSuccessRef.current = onSuccess;

        return {mutate: mutateMock};
    },
}));

const renderForm = (aiProvider: Partial<AiProvider>, showCancel = false) => {
    const onClose = vi.fn();

    render(
        <AiProviderForm
            aiProvider={{id: 7, name: 'openai', ...aiProvider} as AiProvider}
            environment={2}
            onClose={onClose}
            showCancel={showCancel}
        />
    );

    return {onClose};
};

describe('AiProviderForm', () => {
    afterEach(() => {
        resetAll();

        onSuccessRef.current = undefined;
    });

    it('asks only for an API key by default', () => {
        renderForm({});

        expect(screen.getByText('API Key')).toBeInTheDocument();
        expect(screen.queryByText('Endpoint')).not.toBeInTheDocument();
        expect(screen.queryByText('Base URL')).not.toBeInTheDocument();
    });

    it('asks only for a base URL when the provider needs no API key', () => {
        renderForm({requiresApiKey: false});

        expect(screen.getByText('Base URL')).toBeInTheDocument();
        expect(screen.queryByText('API Key')).not.toBeInTheDocument();
    });

    it('asks for both when the provider needs an endpoint as well', () => {
        renderForm({requiresApiKey: true, requiresEndpoint: true});

        expect(screen.getByText('API Key')).toBeInTheDocument();
        expect(screen.getByText('Endpoint')).toBeInTheDocument();
    });

    it('prefills the base URL the provider already has', () => {
        renderForm({requiresApiKey: false, url: 'http://ollama.internal:11434'});

        expect(screen.getByDisplayValue('http://ollama.internal:11434')).toBeInTheDocument();
    });

    it('refuses to submit without the required API key', async () => {
        renderForm({});

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('API Key is required.')).toBeInTheDocument();
        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('refuses to submit without the required endpoint', async () => {
        renderForm({requiresApiKey: true, requiresEndpoint: true});

        await userEvent.type(screen.getByPlaceholderText('API Key'), 'sk-azure');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Endpoint is required.')).toBeInTheDocument();
        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('accepts a blank base URL for a keyless provider, which falls back to the default', async () => {
        renderForm({requiresApiKey: false});

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith({
                environment: 2,
                id: 7,
                updateAiProviderRequest: {url: ''},
            })
        );
    });

    it('sends only the API key for a key-only provider', async () => {
        renderForm({});

        await userEvent.type(screen.getByPlaceholderText('API Key'), 'sk-test');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith({
                environment: 2,
                id: 7,
                updateAiProviderRequest: {apiKey: 'sk-test'},
            })
        );
    });

    it('sends both the API key and the endpoint when one is required', async () => {
        renderForm({requiresApiKey: true, requiresEndpoint: true});

        await userEvent.type(screen.getByPlaceholderText('API Key'), 'sk-azure');
        await userEvent.type(screen.getByPlaceholderText('https://<resource>.openai.azure.com'), 'https://a.example');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith({
                environment: 2,
                id: 7,
                updateAiProviderRequest: {apiKey: 'sk-azure', url: 'https://a.example'},
            })
        );
    });

    it('sends only the URL for a provider that needs no API key', async () => {
        renderForm({requiresApiKey: false});

        await userEvent.type(screen.getByPlaceholderText('http://localhost:11434'), 'http://ollama:11434');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith({
                environment: 2,
                id: 7,
                updateAiProviderRequest: {url: 'http://ollama:11434'},
            })
        );
    });

    it('closes once the update succeeds', () => {
        const {onClose} = renderForm({});

        onSuccessRef.current?.();

        expect(onClose).toHaveBeenCalled();
    });

    it('offers Cancel only when the caller asks for it', async () => {
        const {onClose} = renderForm({}, true);

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onClose).toHaveBeenCalled();
    });

    it('hides Cancel by default', () => {
        renderForm({});

        expect(screen.queryByRole('button', {name: 'Cancel'})).not.toBeInTheDocument();
    });
});
