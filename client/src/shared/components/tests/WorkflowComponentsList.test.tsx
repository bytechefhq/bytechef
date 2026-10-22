import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowComponentsList from '@/shared/components/WorkflowComponentsList';
import {render, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

const componentNames = ['googleMail', 'slack', 'openAi', 'anthropic', 'jira'];

const workflowComponentDefinitions = Object.fromEntries(
    componentNames.map((name) => [name, {icon: '<svg />', name, title: name}])
);

const renderList = (width: number, fitToWidth = true) => {
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue({width} as DOMRect);

    return render(
        <TooltipProvider>
            <WorkflowComponentsList
                filteredComponentNames={componentNames}
                fitToWidth={fitToWidth}
                workflowComponentDefinitions={workflowComponentDefinitions}
                workflowTaskDispatcherDefinitions={{}}
            />
        </TooltipProvider>
    );
};

describe('WorkflowComponentsList', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('shows every icon when they all fit the available width', () => {
        renderList(200);

        expect(screen.getAllByLabelText('Workflow component icon')).toHaveLength(5);
        expect(screen.queryByText(/^\+\d+$/)).not.toBeInTheDocument();
    });

    it('shows only whole icons beside a pill counting the rest when they do not fit', () => {
        renderList(120);

        expect(screen.getAllByLabelText('Workflow component icon')).toHaveLength(2);
        expect(screen.getByText('+3')).toBeInTheDocument();
    });

    it('collapses every icon into the pill when no icon fits beside it', () => {
        renderList(40);

        expect(screen.queryAllByLabelText('Workflow component icon')).toHaveLength(0);
        expect(screen.getByText('+5')).toBeInTheDocument();
    });

    it('ignores the measured width when not fitting to width', () => {
        renderList(40, false);

        expect(screen.getAllByLabelText('Workflow component icon')).toHaveLength(5);
    });
});
