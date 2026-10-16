import {TooltipProvider} from '@/components/ui/tooltip';
import ComponentIconStack from '@/ee/pages/embedded/automation-hub/views/components/ComponentIconStack';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it} from 'vitest';

const createComponents = (count: number) =>
    Array.from({length: count}, (_, index) => ({name: `component${index + 1}`, title: `Component ${index + 1}`}));

const renderStack = (count: number) =>
    render(
        <TooltipProvider>
            <ComponentIconStack components={createComponents(count)} />
        </TooltipProvider>
    );

describe('ComponentIconStack', () => {
    it('shows every icon when there are at most four components', () => {
        renderStack(4);

        expect(screen.getAllByTitle(/^Component \d$/)).toHaveLength(4);
        expect(screen.queryByText(/^\+\d+$/)).not.toBeInTheDocument();
    });

    it('shows three icons and a count of the rest when there are more than four components', () => {
        renderStack(6);

        expect(screen.getAllByTitle(/^Component \d$/).map((icon) => icon.getAttribute('title'))).toEqual([
            'Component 1',
            'Component 2',
            'Component 3',
        ]);
        expect(screen.getByText('+3')).toBeInTheDocument();
    });

    it('lists the hidden components in the tooltip of the count', async () => {
        renderStack(6);

        await userEvent.hover(screen.getByText('+3'));

        const tooltip = await screen.findByRole('tooltip');

        expect(tooltip).toHaveTextContent('Component 4');
        expect(tooltip).toHaveTextContent('Component 5');
        expect(tooltip).toHaveTextContent('Component 6');
        expect(tooltip).not.toHaveTextContent('Component 3');
    });

    it('renders nothing without components', () => {
        const {container} = render(
            <TooltipProvider>
                <ComponentIconStack components={[]} />
            </TooltipProvider>
        );

        expect(container).toBeEmptyDOMElement();
    });
});
