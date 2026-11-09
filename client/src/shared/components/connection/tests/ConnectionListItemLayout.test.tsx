import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionListItemLayout from '../ConnectionListItemLayout';

const renderLayout = (props: Partial<Parameters<typeof ConnectionListItemLayout>[0]> = {}) =>
    render(
        <TooltipProvider>
            <ul>
                <ConnectionListItemLayout
                    menuItems={<button type="button">Custom menu item</button>}
                    name="My Connection"
                    onEditClick={vi.fn()}
                    tagList={<span>tag list</span>}
                    {...props}
                />
            </ul>
        </TooltipProvider>
    );

describe('ConnectionListItemLayout', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
    });

    it('renders the name, tag list, extra badges and children', () => {
        renderLayout({badges: <span>Shared</span>, children: <div>dialog slot</div>});

        expect(screen.getByText('My Connection')).toBeInTheDocument();
        expect(screen.getByText('tag list')).toBeInTheDocument();
        expect(screen.getByText('Shared')).toBeInTheDocument();
        expect(screen.getByText('dialog slot')).toBeInTheDocument();
    });

    it('shows Active for a valid active connection', () => {
        renderLayout({active: true, credentialStatus: 'VALID'});

        expect(screen.getByText('Active')).toBeInTheDocument();
    });

    it('shows Not Active for a valid inactive connection', () => {
        renderLayout({active: false, credentialStatus: 'VALID'});

        expect(screen.getByText('Not Active')).toBeInTheDocument();
    });

    it('shows the credential status when it is not valid', () => {
        renderLayout({active: true, credentialStatus: 'REJECTED'});

        expect(screen.getByText('REJECTED')).toBeInTheDocument();
        expect(screen.queryByText('Active')).not.toBeInTheDocument();
    });

    it('shows INVALID when the credential status is missing', () => {
        renderLayout();

        expect(screen.getByText('INVALID')).toBeInTheDocument();
    });

    it('shows the created date only when present', () => {
        const {unmount} = renderLayout();

        expect(screen.queryByText(/Created at/)).not.toBeInTheDocument();

        unmount();

        renderLayout({createdDate: new Date(2026, 10, 9, 18, 0, 0)});

        expect(screen.getByText(/Created at/)).toBeInTheDocument();
    });

    it('opens the menu with the given items and calls onEditClick', async () => {
        const onEditClick = vi.fn();

        renderLayout({onEditClick});

        await userEvent.click(screen.getByRole('button', {name: ''}));

        expect(screen.getByText('Custom menu item')).toBeInTheDocument();

        await userEvent.click(screen.getByText('Edit'));

        expect(onEditClick).toHaveBeenCalledTimes(1);
    });
});
