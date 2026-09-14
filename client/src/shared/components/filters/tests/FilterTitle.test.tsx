import FilterTitle from '@/shared/components/filters/FilterTitle';
import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

describe('FilterTitle', () => {
    it('renders a badge for every filter that has a value', () => {
        render(
            <FilterTitle
                filters={[
                    {label: 'Status', value: 'COMPLETED'},
                    {label: 'Project', value: 'AI Agent'},
                    {label: 'Workflow', value: 'workflow1'},
                ]}
            />
        );

        expect(screen.getByText('Filter by:')).toBeInTheDocument();
        expect(screen.getByText('Status: COMPLETED')).toBeInTheDocument();
        expect(screen.getByText('Project: AI Agent')).toBeInTheDocument();
        expect(screen.getByText('Workflow: workflow1')).toBeInTheDocument();
        expect(screen.queryByText('none')).not.toBeInTheDocument();
    });

    it('skips filters without a value', () => {
        render(
            <FilterTitle
                filters={[
                    {label: 'Status', value: 'FAILED'},
                    {label: 'Project', value: undefined},
                ]}
            />
        );

        expect(screen.getByText('Status: FAILED')).toBeInTheDocument();
        expect(screen.queryByText(/Project:/)).not.toBeInTheDocument();
    });

    it('renders none when no filter has a value', () => {
        render(<FilterTitle filters={[{label: 'Status', value: undefined}]} />);

        expect(screen.getByText('none')).toBeInTheDocument();
    });
});
