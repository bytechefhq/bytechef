import Button from '@/components/Button/Button';
import {render, screen, within} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {createPortal} from 'react-dom';
import {describe, expect, it, vi} from 'vitest';

import JourneySteps, {type JourneyStepI} from './JourneySteps';

const steps: JourneyStepI[] = [
    {id: 'createProject', label: 'Create a project', status: 'complete'},
    {id: 'buildWorkflow', label: 'Build and test a workflow', status: 'current'},
    {id: 'publish', label: 'Publish a version', status: 'upcoming'},
];

it('should render one list item per step in order', () => {
    render(<JourneySteps aria-label="Getting started" steps={steps} />);

    const items = within(screen.getByRole('list', {name: 'Getting started'})).getAllByRole('listitem');

    expect(items.map((item) => item.textContent)).toEqual([
        expect.stringContaining('Create a project'),
        expect.stringContaining('Build and test a workflow'),
        expect.stringContaining('Publish a version'),
    ]);
});

it('should mark only the current step with aria-current', () => {
    render(<JourneySteps steps={steps} />);

    const items = screen.getAllByRole('listitem');

    expect(items[0]).not.toHaveAttribute('aria-current');
    expect(items[1]).toHaveAttribute('aria-current', 'step');
    expect(items[2]).not.toHaveAttribute('aria-current');
});

it('should replace the number of a complete step with a check and announce it as completed', () => {
    const {container} = render(<JourneySteps steps={steps} />);

    const [completeItem, currentItem, upcomingItem] = screen.getAllByRole('listitem');

    expect(completeItem.querySelector('svg.lucide-check')).toBeInTheDocument();
    expect(within(completeItem).getByText('Completed:')).toHaveClass('sr-only');
    expect(within(currentItem).getByText('2')).toBeInTheDocument();
    expect(within(upcomingItem).getByText('3')).toBeInTheDocument();
    expect(container.querySelectorAll('svg.lucide-check')).toHaveLength(1);
});

it('should render a step description and action when given', () => {
    render(
        <JourneySteps
            orientation="vertical"
            steps={[
                {
                    action: <Button label="Create project" />,
                    description: 'Projects group related workflows.',
                    id: 'createProject',
                    label: 'Create a project',
                    status: 'current',
                },
                {id: 'publish', label: 'Publish a version', status: 'upcoming'},
            ]}
        />
    );

    const [currentItem] = screen.getAllByRole('listitem');

    expect(within(currentItem).getByText('Projects group related workflows.')).toBeInTheDocument();
    expect(within(currentItem).getByRole('button', {name: 'Create project'})).toBeInTheDocument();
});

it('should render step content below the description in vertical orientation', () => {
    render(
        <JourneySteps
            orientation="vertical"
            steps={[
                {
                    content: <div data-testid="step-content">Listening for events</div>,
                    description: 'Projects group related workflows.',
                    id: 'createProject',
                    label: 'Create a project',
                    status: 'current',
                },
            ]}
        />
    );

    const content = screen.getByTestId('step-content');
    const description = screen.getByText('Projects group related workflows.');

    expect(screen.getByRole('listitem')).toContainElement(content);
    expect(description.compareDocumentPosition(content) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(content.parentElement).toHaveClass('col-span-2', 'col-start-2');
});

it('should ignore step content in horizontal orientation', () => {
    render(
        <JourneySteps
            steps={[
                {
                    content: <div data-testid="step-content">Listening for events</div>,
                    id: 'createProject',
                    label: 'Create a project',
                    status: 'current',
                },
            ]}
        />
    );

    expect(screen.queryByTestId('step-content')).not.toBeInTheDocument();
});

describe('JourneySteps status tokens', () => {
    it('should use success tokens for a complete step marker', () => {
        render(<JourneySteps steps={steps} />);

        const marker = screen.getAllByRole('listitem')[0].querySelector('svg.lucide-check')?.parentElement;

        expect(marker).toHaveClass('bg-surface-success-secondary text-content-success-primary');
    });

    it('should use brand tokens for the current step marker', () => {
        render(<JourneySteps steps={steps} />);

        expect(within(screen.getAllByRole('listitem')[1]).getByText('2')).toHaveClass(
            'bg-surface-brand-primary text-content-onsurface-primary'
        );
    });
});

describe('TypeScript tests', () => {
    it('should merge className via twMerge', () => {
        render(<JourneySteps className="mt-8" steps={steps} />);

        expect(screen.getByRole('list')).toHaveClass('mt-8');
    });

    it('should not allow an unknown step status', () => {
        // @ts-expect-error - status must be complete, current or upcoming
        render(<JourneySteps steps={[{id: 'publish', label: 'Publish a version', status: 'done'}]} />);
    });

    it('should toggle a step when its row is clicked, but not when its content is clicked', async () => {
        const onToggle = vi.fn();

        render(
            <JourneySteps
                orientation="vertical"
                steps={[
                    {
                        content: <span>step details</span>,
                        expanded: true,
                        id: 'done',
                        label: 'Done step',
                        onToggle,
                        status: 'complete',
                    },
                ]}
            />
        );

        const toggle = screen.getByRole('button', {name: /Done step/});

        expect(toggle).toHaveAttribute('aria-expanded', 'true');

        await userEvent.click(screen.getByText('step details'));

        expect(onToggle).not.toHaveBeenCalled();

        await userEvent.click(toggle);

        expect(onToggle).toHaveBeenCalledTimes(1);

        toggle.focus();

        await userEvent.keyboard('{Enter}');

        expect(onToggle).toHaveBeenCalledTimes(2);
    });

    it('should not toggle a step when a portal rendered from it is clicked', async () => {
        const onToggle = vi.fn();

        render(
            <JourneySteps
                orientation="vertical"
                steps={[
                    {
                        content: createPortal(<button type="button">inside dialog</button>, document.body),
                        expanded: true,
                        id: 'withDialog',
                        label: 'Step with dialog',
                        onToggle,
                        status: 'current',
                    },
                ]}
            />
        );

        await userEvent.click(screen.getByRole('button', {name: 'inside dialog'}));

        expect(onToggle).not.toHaveBeenCalled();
    });

    it('should not make a step without a toggle interactive', () => {
        render(<JourneySteps orientation="vertical" steps={steps} />);

        expect(screen.queryByRole('button', {name: /Create a project/})).not.toBeInTheDocument();
    });

    it('should not allow an unknown orientation', () => {
        // @ts-expect-error - orientation must be horizontal or vertical
        render(<JourneySteps orientation="diagonal" steps={steps} />);
    });
});
