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

    it('asks for a base URL and an optional API key when the provider needs no API key', () => {
        renderForm({requiresApiKey: false});

        expect(screen.getByText('Base URL')).toBeInTheDocument();
        expect(screen.getByText('API Key')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Optional')).toBeInTheDocument();
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

    it('sends the API key with the URL when one is entered for a provider that needs no API key', async () => {
        renderForm({requiresApiKey: false});

        await userEvent.type(screen.getByPlaceholderText('http://localhost:11434'), 'http://ollama:11434');
        await userEvent.type(screen.getByPlaceholderText('Optional'), 'secret-token');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith({
                environment: 2,
                id: 7,
                updateAiProviderRequest: {apiKey: 'secret-token', url: 'http://ollama:11434'},
            })
        );
    });

    it('prefills the masked API key a provider that needs no API key already has', () => {
        renderForm({apiKey: '..........................ret123', requiresApiKey: false});

        expect(screen.getByDisplayValue('..........................ret123')).toBeInTheDocument();
    });

    it('keeps the saved API key when the masked key is left unchanged', async () => {
        renderForm({apiKey: '..........................ret123', requiresApiKey: false, url: 'http://ollama:11434'});

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith({
                environment: 2,
                id: 7,
                updateAiProviderRequest: {url: 'http://ollama:11434'},
            })
        );
    });

    it('removes the saved API key when the masked key is cleared', async () => {
        renderForm({apiKey: '..........................ret123', requiresApiKey: false, url: 'http://ollama:11434'});

        await userEvent.clear(screen.getByPlaceholderText('Optional'));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith({
                environment: 2,
                id: 7,
                updateAiProviderRequest: {apiKey: '', url: 'http://ollama:11434'},
            })
        );
    });

    it('prefills the masked API key a provider already has', () => {
        renderForm({apiKey: '..........................sk1234'});

        expect(screen.getByDisplayValue('..........................sk1234')).toBeInTheDocument();
    });

    it('keeps the saved API key when the masked key is left unchanged', async () => {
        renderForm({apiKey: '..........................sk1234'});

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith({
                environment: 2,
                id: 7,
                updateAiProviderRequest: {},
            })
        );
    });

    it('sends only a new endpoint when the masked API key is left unchanged', async () => {
        renderForm({
            apiKey: '..........................sk1234',
            requiresApiKey: true,
            requiresEndpoint: true,
            url: 'https://a.example',
        });

        await userEvent.clear(screen.getByPlaceholderText('https://<resource>.openai.azure.com'));
        await userEvent.type(screen.getByPlaceholderText('https://<resource>.openai.azure.com'), 'https://b.example');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(mutateMock).toHaveBeenCalledWith({
                environment: 2,
                id: 7,
                updateAiProviderRequest: {url: 'https://b.example'},
            })
        );
    });

    it('refuses to submit when the masked API key of a provider that requires one is cleared', async () => {
        renderForm({apiKey: '..........................sk1234'});

        await userEvent.clear(screen.getByPlaceholderText('API Key'));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('API Key is required.')).toBeInTheDocument();
        expect(mutateMock).not.toHaveBeenCalled();
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
