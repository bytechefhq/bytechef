import Button from '@/components/Button/Button';
import {render, screen} from '@/shared/util/test-utils';
import {FolderIcon} from 'lucide-react';
import {describe, expect, it} from 'vitest';

import EmptyState from './EmptyState';

it('should render the title as a level 2 heading', () => {
    render(<EmptyState icon={<FolderIcon />} title="Create your first project" />);

    expect(screen.getByRole('heading', {level: 2, name: 'Create your first project'})).toBeInTheDocument();
});

it('should render the icon inside a brand-tinted tile', () => {
    const {container} = render(<EmptyState icon={<FolderIcon />} title="Create your first project" />);

    const icon = container.querySelector('svg.lucide-folder');

    expect(icon).toBeInTheDocument();
    expect(icon?.parentElement).toHaveClass('bg-surface-brand-secondary text-content-brand-primary');
});

it('should render the description in secondary content color', () => {
    render(
        <EmptyState
            description="A project groups related workflows."
            icon={<FolderIcon />}
            title="Create your first project"
        />
    );

    expect(screen.getByText('A project groups related workflows.')).toHaveClass('text-content-neutral-secondary');
});

it('should render actions, children and the note', () => {
    render(
        <EmptyState
            actions={<Button label="Create Project" />}
            icon={<FolderIcon />}
            note="Deployments are separate for each environment."
            title="Create your first project"
        >
            <ol aria-label="Getting started" />
        </EmptyState>
    );

    expect(screen.getByRole('button', {name: 'Create Project'})).toBeInTheDocument();
    expect(screen.getByRole('list', {name: 'Getting started'})).toBeInTheDocument();
    expect(screen.getByText('Deployments are separate for each environment.')).toBeInTheDocument();
});

it('should open the docs link in a new tab without leaking the opener', () => {
    render(
        <EmptyState
            docsLink={{
                href: 'https://docs.bytechef.io/platform/automation/build/connections',
                label: 'How connections work',
            }}
            icon={<FolderIcon />}
            title="Connect your first app"
        />
    );

    const docsLink = screen.getByRole('link', {name: 'How connections work'});

    expect(docsLink).toHaveAttribute('href', 'https://docs.bytechef.io/platform/automation/build/connections');
    expect(docsLink).toHaveAttribute('target', '_blank');
    expect(docsLink).toHaveAttribute('rel', 'noopener noreferrer');
});

it('should not render an empty content section when only the header is given', () => {
    const {container} = render(<EmptyState icon={<FolderIcon />} title="Create your first project" />);

    expect(container.querySelector('[data-slot="empty-content"]')).not.toBeInTheDocument();
});

describe('TypeScript tests', () => {
    it('should merge className via twMerge', () => {
        const {container} = render(
            <EmptyState className="p-2" icon={<FolderIcon />} title="Create your first project" />
        );

        const root = container.querySelector('[data-slot="empty"]');

        expect(root).toHaveClass('p-2');
        expect(root).not.toHaveClass('p-6');
    });

    it('should require a title', () => {
        // @ts-expect-error - title is required
        render(<EmptyState icon={<FolderIcon />} />);
    });

    it('should require an icon', () => {
        // @ts-expect-error - icon is required
        render(<EmptyState title="Create your first project" />);
    });
});
