import {TooltipProvider} from '@/components/ui/tooltip';
import {cleanup, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, describe, expect, it, vi} from 'vitest';

import SubStepList from '../SubStepList';

const renderList = (copyValue?: string) =>
    render(
        <TooltipProvider>
            <MemoryRouter>
                <SubStepList
                    ariaLabel="Sub steps"
                    subSteps={[{copyValue, done: false, hint: 'A hint', href: '/somewhere', id: 'one', label: 'One'}]}
                />
            </MemoryRouter>
        </TooltipProvider>
    );

describe('SubStepList', () => {
    afterEach(() => {
        cleanup();
    });

    it('renders a copy button only for a sub-step with a copy value', () => {
        const {unmount} = renderList('value-to-copy');

        expect(screen.getByRole('button')).toBeInTheDocument();

        unmount();

        renderList();

        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('renders a disabled sub-step as plain text and ignores its action', () => {
        const action = vi.fn();

        render(
            <TooltipProvider>
                <MemoryRouter>
                    <SubStepList
                        ariaLabel="Sub steps"
                        subStepActions={{one: action}}
                        subSteps={[
                            {disabled: true, done: false, hint: 'A hint', href: '/somewhere', id: 'one', label: 'One'},
                        ]}
                    />
                </MemoryRouter>
            </TooltipProvider>
        );

        expect(screen.queryByRole('link')).not.toBeInTheDocument();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
        expect(screen.getByText('One')).toHaveAttribute('aria-disabled', 'true');
        expect(action).not.toHaveBeenCalled();
    });
});
