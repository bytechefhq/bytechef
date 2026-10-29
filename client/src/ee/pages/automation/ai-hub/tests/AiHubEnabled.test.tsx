import AiHubEnabled from '@/ee/pages/automation/ai-hub/AiHubEnabled';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {render, screen} from '@testing-library/react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it} from 'vitest';

const initialState = applicationInfoStore.getState();

function setApplicationInfo(aiHubEnabled: boolean, loaded = true) {
    applicationInfoStore.setState({
        ai: {...initialState.ai, hub: {enabled: aiHubEnabled}},
        application: loaded ? {edition: 'EE'} : null,
    } as Partial<ReturnType<typeof applicationInfoStore.getState>>);
}

function renderHubRoute() {
    return render(
        <MemoryRouter initialEntries={['/automation/ai-hub']}>
            <Routes>
                <Route element={<div>Projects page</div>} path="/automation/projects" />

                <Route
                    element={
                        <AiHubEnabled>
                            <div>AI Hub page</div>
                        </AiHubEnabled>
                    }
                    path="/automation/ai-hub"
                />
            </Routes>
        </MemoryRouter>
    );
}

describe('AiHubEnabled', () => {
    beforeEach(() => {
        applicationInfoStore.setState(initialState, true);
    });

    it('renders the hub when it is enabled', () => {
        setApplicationInfo(true);

        renderHubRoute();

        expect(screen.getByText('AI Hub page')).toBeInTheDocument();
    });

    it('redirects to projects when the hub is disabled', () => {
        setApplicationInfo(false);

        renderHubRoute();

        expect(screen.queryByText('AI Hub page')).not.toBeInTheDocument();
        expect(screen.getByText('Projects page')).toBeInTheDocument();
    });

    it('renders nothing until application info has loaded', () => {
        setApplicationInfo(true, false);

        renderHubRoute();

        expect(screen.queryByText('AI Hub page')).not.toBeInTheDocument();
        expect(screen.queryByText('Projects page')).not.toBeInTheDocument();
    });
});
