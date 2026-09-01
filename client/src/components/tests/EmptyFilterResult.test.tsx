import EmptyFilterResult from '@/components/EmptyFilterResult';
import {render, screen} from '@testing-library/react';
import {expect, it} from 'vitest';

// The point of the component: a filtered list that comes back empty used to render the first-run empty state,
// which told the reader the workspace was empty and offered to create the thing they already had.
it('names what was filtered rather than offering to create one', () => {
    render(<EmptyFilterResult entityName="project deployments" entityTitle="Project Deployments" />);

    expect(screen.getByText('No Matching Project Deployments')).toBeInTheDocument();
    expect(screen.getByText('No project deployments match the current filter.')).toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

it("carries the caller's casing into the title and the message separately", () => {
    render(<EmptyFilterResult entityName="API collections" entityTitle="API Collections" />);

    expect(screen.getByText('No Matching API Collections')).toBeInTheDocument();
    expect(screen.getByText('No API collections match the current filter.')).toBeInTheDocument();
});
