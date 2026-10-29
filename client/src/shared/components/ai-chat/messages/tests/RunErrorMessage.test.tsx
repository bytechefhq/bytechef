import RunErrorMessage from '@/shared/components/ai-chat/messages/RunErrorMessage';
import {RunErrorDataI} from '@/shared/components/ai-chat/messages/toRunErrorData';
import {DataMessagePartProps} from '@assistant-ui/react';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it} from 'vitest';

const renderRunError = (data: RunErrorDataI) =>
    render(
        <MemoryRouter>
            <RunErrorMessage {...({data} as DataMessagePartProps<RunErrorDataI>)} />
        </MemoryRouter>
    );

describe('RunErrorMessage', () => {
    it('links to the AI Providers settings when no AI provider is set up', () => {
        renderRunError({code: 'NO_AI_PROVIDER', message: 'No AI provider is set up for this environment.'});

        expect(screen.getByRole('link', {name: 'Open AI Providers'})).toHaveAttribute(
            'href',
            '/automation/settings/ai-providers'
        );
    });

    it('shows only the message for other run errors', () => {
        renderRunError({message: 'catalog down'});

        expect(screen.getByRole('alert')).toHaveTextContent('catalog down');
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
    });
});
