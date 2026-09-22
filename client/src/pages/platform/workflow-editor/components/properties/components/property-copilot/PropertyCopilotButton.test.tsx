import {PropertyCopilotMode} from '@/shared/middleware/graphql-types';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCopilotButton from './PropertyCopilotButton';

const generateMock = vi.fn();
const copilotEnabledMock = vi.fn();
const hasEnabledAiProviderMock = vi.fn();

type GenerateResultType = {message: string | null; valid: boolean; value: string};

const createDeferredResult = () => {
    let resolveResult: (result: GenerateResultType) => void = () => {};

    const promise = new Promise<GenerateResultType>((resolve) => {
        resolveResult = resolve;
    });

    return {promise, resolveResult};
};

vi.mock('./useGeneratePropertyValue', () => ({
    useGeneratePropertyValue: () => ({generate: generateMock, isPending: false}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: unknown) => unknown) =>
        selector({ai: {copilot: {enabled: copilotEnabledMock()}}}),
}));

vi.mock('@/shared/hooks/useHasEnabledAiProvider', () => ({
    useHasEnabledAiProvider: () => hasEnabledAiProviderMock(),
}));

vi.mock('@/components/ui/tooltip', () => ({
    Tooltip: ({children}: {children: ReactNode}) => <>{children}</>,
    TooltipContent: ({children}: {children: ReactNode}) => <div role="tooltip">{children}</div>,
    TooltipTrigger: ({children}: {children: ReactNode}) => <>{children}</>,
}));

describe('PropertyCopilotButton', () => {
    beforeEach(() => {
        generateMock.mockReset();
        copilotEnabledMock.mockReset().mockReturnValue(true);
        hasEnabledAiProviderMock.mockReset().mockReturnValue({hasEnabledAiProvider: true, isPending: false});
    });

    const baseProps = {
        dynamic: true,
        environmentId: 0,
        getHasValue: () => false,
        mode: PropertyCopilotMode.Text,
        onApply: vi.fn(),
        propertyPath: 'p',
        propertyType: 'STRING',
        workflowId: 'wf1',
        workflowNodeName: 'n1',
    };

    it('is hidden when copilot is disabled', () => {
        copilotEnabledMock.mockReturnValue(false);

        render(<PropertyCopilotButton {...baseProps} />);

        expect(screen.queryByLabelText(/copilot/i)).not.toBeInTheDocument();
    });

    it('is disabled and does not open the popover when disabled', () => {
        render(<PropertyCopilotButton {...baseProps} disabled />);

        const button = screen.getByLabelText(/copilot/i);

        expect(button).toBeDisabled();

        fireEvent.click(button);

        expect(screen.queryByPlaceholderText(/describe/i)).not.toBeInTheDocument();
    });

    it('passes the dynamic flag through to generate', async () => {
        generateMock.mockResolvedValue({message: null, valid: true, value: 'x'});

        render(<PropertyCopilotButton {...baseProps} dynamic={false} />);

        fireEvent.click(screen.getByLabelText(/copilot/i));
        fireEvent.change(screen.getByPlaceholderText(/describe/i), {target: {value: 'greet'}});
        fireEvent.click(screen.getByRole('button', {name: /generate/i}));

        await waitFor(() => expect(generateMock).toHaveBeenCalledWith(expect.objectContaining({dynamic: false})));
    });

    it('previews the generated value and applies it on Insert', async () => {
        generateMock.mockResolvedValue({message: null, valid: true, value: 'Hi ${n1.name}'});
        const onApply = vi.fn();

        render(<PropertyCopilotButton {...baseProps} onApply={onApply} />);

        fireEvent.click(screen.getByLabelText(/copilot/i));
        fireEvent.change(screen.getByPlaceholderText(/describe/i), {target: {value: 'greet'}});
        fireEvent.click(screen.getByRole('button', {name: /generate/i}));

        await waitFor(() => expect(screen.getByText('Hi ${n1.name}')).toBeInTheDocument());
        expect(generateMock).toHaveBeenCalledWith(
            expect.objectContaining({mode: PropertyCopilotMode.Text, prompt: 'greet'})
        );
        expect(onApply).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', {name: /insert/i}));

        expect(onApply).toHaveBeenCalledWith('Hi ${n1.name}');
    });

    it('offers Replace when the field already has a value', async () => {
        generateMock.mockResolvedValue({message: null, valid: true, value: 'new value'});

        render(<PropertyCopilotButton {...baseProps} getHasValue={() => true} />);

        fireEvent.click(screen.getByLabelText(/copilot/i));
        fireEvent.change(screen.getByPlaceholderText(/describe/i), {target: {value: 'greet'}});
        fireEvent.click(screen.getByRole('button', {name: /generate/i}));

        await waitFor(() => expect(screen.getByRole('button', {name: /replace/i})).toBeInTheDocument());
    });

    it('ignores an earlier response that resolves after a later Generate click', async () => {
        const firstResult = createDeferredResult();
        const secondResult = createDeferredResult();

        generateMock.mockReturnValueOnce(firstResult.promise).mockReturnValueOnce(secondResult.promise);

        render(<PropertyCopilotButton {...baseProps} />);

        fireEvent.click(screen.getByLabelText(/copilot/i));
        fireEvent.change(screen.getByPlaceholderText(/describe/i), {target: {value: 'greet'}});
        fireEvent.click(screen.getByRole('button', {name: /generate/i}));
        fireEvent.click(screen.getByRole('button', {name: /generate/i}));

        await act(async () => {
            secondResult.resolveResult({message: null, valid: true, value: 'second value'});
        });

        await act(async () => {
            firstResult.resolveResult({message: null, valid: true, value: 'first value'});
        });

        expect(screen.getByText('second value')).toBeInTheDocument();
        expect(screen.queryByText('first value')).not.toBeInTheDocument();
    });

    it('ignores a response that resolves after the popover was closed', async () => {
        const pendingResult = createDeferredResult();

        generateMock.mockReturnValueOnce(pendingResult.promise);

        render(<PropertyCopilotButton {...baseProps} />);

        fireEvent.click(screen.getByLabelText(/copilot/i));
        fireEvent.change(screen.getByPlaceholderText(/describe/i), {target: {value: 'greet'}});
        fireEvent.click(screen.getByRole('button', {name: /generate/i}));

        fireEvent.click(screen.getByLabelText(/copilot/i));
        fireEvent.click(screen.getByLabelText(/copilot/i));

        await act(async () => {
            pendingResult.resolveResult({message: null, valid: true, value: 'stale value'});
        });

        expect(screen.queryByText('stale value')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /insert/i})).not.toBeInTheDocument();
    });

    it('is disabled with an explanation when no AI provider is enabled', () => {
        hasEnabledAiProviderMock.mockReturnValue({hasEnabledAiProvider: false, isPending: false});

        render(<PropertyCopilotButton {...baseProps} />);

        const button = screen.getByLabelText(/copilot/i);

        expect(button).toBeDisabled();
        expect(screen.getByRole('tooltip')).toHaveTextContent('Enable an AI provider to use Copilot.');

        fireEvent.click(button);

        expect(screen.queryByPlaceholderText(/describe/i)).not.toBeInTheDocument();
    });

    it('stays enabled while the AI provider check is pending', () => {
        hasEnabledAiProviderMock.mockReturnValue({hasEnabledAiProvider: false, isPending: true});

        render(<PropertyCopilotButton {...baseProps} />);

        expect(screen.getByLabelText(/copilot/i)).toBeEnabled();
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });
});
