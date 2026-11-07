import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import ListItemExpandButton from '@/shared/components/ListItemExpandButton';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

const renderInRow = ({disabled, onRowClick = vi.fn()}: {disabled?: boolean; onRowClick?: () => void}) =>
    render(
        <Collapsible>
            <div onClick={onRowClick} role="presentation">
                <ListItemExpandButton disabled={disabled} label="Toggle project" />
            </div>

            <CollapsibleContent>Project workflows</CollapsibleContent>
        </Collapsible>
    );

describe('ListItemExpandButton', () => {
    it('opens and closes the list item it belongs to', async () => {
        renderInRow({});

        const expandButton = screen.getByRole('button', {name: 'Toggle project'});

        expect(expandButton).toHaveAttribute('aria-expanded', 'false');
        expect(screen.queryByText('Project workflows')).not.toBeInTheDocument();

        await userEvent.click(expandButton);

        expect(expandButton).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByText('Project workflows')).toBeInTheDocument();

        await userEvent.click(expandButton);

        expect(expandButton).toHaveAttribute('aria-expanded', 'false');
    });

    it('does not pass its click on to the row, which would toggle the list item a second time', async () => {
        const onRowClick = vi.fn();

        renderInRow({onRowClick});

        await userEvent.click(screen.getByRole('button', {name: 'Toggle project'}));

        expect(onRowClick).not.toHaveBeenCalled();
    });

    it('stays closed when disabled', async () => {
        renderInRow({disabled: true});

        const expandButton = screen.getByRole('button', {name: 'Toggle project'});

        expect(expandButton).toBeDisabled();

        await userEvent.click(expandButton);

        expect(screen.queryByText('Project workflows')).not.toBeInTheDocument();
    });
});
