import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import McpServerToolsSections from '../McpServerToolsSections';

const renderSections = (showComponentList: boolean, showWorkflowList: boolean) =>
    render(
        <McpServerToolsSections
            componentList={<div>Component list</div>}
            showComponentList={showComponentList}
            showWorkflowList={showWorkflowList}
            workflowList={<div>Workflow list</div>}
        />
    );

describe('McpServerToolsSections', () => {
    it('separates components and workflows under their own headings', () => {
        renderSections(true, true);

        expect(screen.getByRole('heading', {name: 'Components'})).toBeInTheDocument();
        expect(screen.getByRole('heading', {name: 'Workflows'})).toBeInTheDocument();
        expect(screen.getByRole('region', {name: 'Components'})).toHaveTextContent('Component list');
        expect(screen.getByRole('region', {name: 'Workflows'})).toHaveTextContent('Workflow list');
    });

    it('omits the Workflows heading when there are no workflows', () => {
        renderSections(true, false);

        expect(screen.getByRole('heading', {name: 'Components'})).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'Workflows'})).not.toBeInTheDocument();
    });

    it('omits the Components heading when there are no components', () => {
        renderSections(false, true);

        expect(screen.queryByRole('heading', {name: 'Components'})).not.toBeInTheDocument();
        expect(screen.getByRole('heading', {name: 'Workflows'})).toBeInTheDocument();
    });
});
