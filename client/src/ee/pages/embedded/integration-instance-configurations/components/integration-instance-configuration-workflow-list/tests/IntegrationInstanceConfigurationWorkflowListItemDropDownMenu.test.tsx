import {Workflow} from '@/shared/middleware/automation/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationWorkflowListItemDropDownMenu from '../IntegrationInstanceConfigurationWorkflowListItemDropDownMenu';

const onEditClick = vi.fn();

const openMenu = async (workflow: Partial<Workflow> = {connectionsCount: 1}) => {
    render(
        <IntegrationInstanceConfigurationWorkflowListItemDropDownMenu
            onEditClick={onEditClick}
            workflow={workflow as Workflow}
        />
    );

    await userEvent.click(screen.getByRole('button'));
};

describe('IntegrationInstanceConfigurationWorkflowListItemDropDownMenu', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('should render with the design-system DropdownMenu', async () => {
        await openMenu();

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toHaveClass('text-content-neutral-primary');
    });

    it('should call onEditClick when Edit is clicked', async () => {
        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(onEditClick).toHaveBeenCalledTimes(1);
    });

    it('should disable Edit when the workflow has no connections and no inputs', async () => {
        await openMenu({connectionsCount: 0, inputsCount: 0});

        expect(screen.getByRole('menuitem', {name: 'Edit'})).toHaveAttribute('data-disabled');
    });

    it.each([[{connectionsCount: 0, inputsCount: 1}], [{connectionsCount: 2, inputsCount: 0}]])(
        'should keep Edit enabled for the workflow %o',
        async (workflow) => {
            await openMenu(workflow);

            expect(screen.getByRole('menuitem', {name: 'Edit'})).not.toHaveAttribute('data-disabled');
        }
    );
});
