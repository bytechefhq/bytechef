import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it} from 'vitest';

import ExecutionsTabs from './ExecutionsTabs';

const LocationDisplay = () => {
    const {pathname} = useLocation();

    return <span data-testid="location">{pathname}</span>;
};

const renderTabs = (initialPath: string, fallback?: string) =>
    render(
        <MemoryRouter initialEntries={[initialPath]}>
            <Routes>
                <Route
                    element={
                        <>
                            <ExecutionsTabs fallback={fallback} />

                            <LocationDisplay />
                        </>
                    }
                    path="*"
                />
            </Routes>
        </MemoryRouter>
    );

const setEdition = (edition: EditionType) => applicationInfoStore.setState({application: {edition}});

describe('ExecutionsTabs', () => {
    beforeEach(() => {
        setEdition(EditionType.EE);
    });

    it('selects the Workflow Executions tab on the executions route', () => {
        renderTabs('/automation/executions');

        expect(screen.getByRole('tab', {name: 'Workflow Executions'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByRole('tab', {name: 'Tool Invocations'})).toHaveAttribute('aria-selected', 'false');
    });

    it('selects the Tool Invocations tab on the tool invocations route', () => {
        renderTabs('/embedded/executions/tool-invocations');

        expect(screen.getByRole('tab', {name: 'Tool Invocations'})).toHaveAttribute('aria-selected', 'true');
    });

    it('navigates between the automation executions routes', async () => {
        renderTabs('/automation/executions');

        await userEvent.click(screen.getByRole('tab', {name: 'Tool Invocations'}));

        expect(screen.getByTestId('location')).toHaveTextContent('/automation/executions/tool-invocations');

        await userEvent.click(screen.getByRole('tab', {name: 'Workflow Executions'}));

        expect(screen.getByTestId('location')).toHaveTextContent(/^\/automation\/executions$/);
    });

    it('stays within the embedded executions routes', async () => {
        renderTabs('/embedded/executions');

        await userEvent.click(screen.getByRole('tab', {name: 'Tool Invocations'}));

        expect(screen.getByTestId('location')).toHaveTextContent('/embedded/executions/tool-invocations');
    });

    it('renders the fallback instead of tabs in CE, where Tool Invocations is unavailable', () => {
        setEdition(EditionType.CE);

        renderTabs('/automation/executions', 'Workflow Executions');

        expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
        expect(screen.getByText('Workflow Executions')).toBeInTheDocument();
    });
});
