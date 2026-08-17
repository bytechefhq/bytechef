import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

/**
 * Coverage for the composer's '/' skills menu: keyboard navigation (the primary path — the user's hands are
 * already on the keys when the menu opens), multi-select into the composer store, and the composer-text
 * teardown that follows a pick.
 *
 * The assistant-ui composer is faked with a tiny observable text store so a test can drive the menu the way
 * typing does — the menu derives its open state from that text, never from a click.
 */

const {composerRef} = vi.hoisted(() => {
    const listeners = new Set<() => void>();

    return {
        composerRef: {
            listeners,
            setText: (text: string) => {
                composerRef.text = text;

                listeners.forEach((listener) => listener());
            },
            text: '',
        } as {listeners: Set<() => void>; setText: (text: string) => void; text: string},
    };
});

vi.mock('@assistant-ui/react', () => ({
    useAui: () => ({
        composer: {
            getState: () => ({text: composerRef.text}),
            setText: (text: string) => composerRef.setText(text),
        },
        subscribe: (listener: () => void) => {
            composerRef.listeners.add(listener);

            return () => composerRef.listeners.delete(listener);
        },
    }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiSkillsQuery: () => ({
        data: {
            aiSkills: [
                {description: 'Fetches unread emails', id: 'skill-1', name: 'email-digest'},
                {description: 'Echo health check', id: 'skill-2', name: 'test'},
                {description: 'Logs leads', id: 'skill-3', name: 'logLeads'},
            ],
        },
    }),
}));

const renderMenu = async () => {
    const {default: AiHubSkillsMenu} = await import('../AiHubSkillsMenu');

    const result = render(
        <TooltipProvider>
            <AiHubSkillsMenu />

            <textarea aria-label="Message input" />
        </TooltipProvider>
    );

    return result;
};

const getComposerInput = () => screen.getByLabelText('Message input');

beforeEach(() => {
    aiHubComposerStore.setState({referencedResources: [], selectedSkills: []});

    composerRef.text = '';
    composerRef.listeners.clear();
});

describe('AiHubSkillsMenu', () => {
    it('opens on a leading slash and filters as the user types', async () => {
        await renderMenu();

        composerRef.setText('/');

        await waitFor(() => {
            expect(screen.getByText('email-digest')).toBeInTheDocument();
        });

        composerRef.setText('/log');

        await waitFor(() => {
            expect(screen.queryByText('email-digest')).not.toBeInTheDocument();
        });

        expect(screen.getByText('logLeads')).toBeInTheDocument();
    });

    it('picks the highlighted skill with Enter after moving the highlight with the arrow keys', async () => {
        await renderMenu();

        composerRef.setText('/');

        await waitFor(() => {
            expect(screen.getByText('email-digest')).toBeInTheDocument();
        });

        fireEvent.keyDown(getComposerInput(), {key: 'ArrowDown'});
        fireEvent.keyDown(getComposerInput(), {key: 'Enter'});

        await waitFor(() => {
            expect(aiHubComposerStore.getState().selectedSkills).toEqual([{id: 'skill-2', name: 'test'}]);
        });

        // The slash command is consumed rather than rewritten into the message.
        expect(composerRef.text).toBe('');
    });

    it('accumulates several skills across separate slash commands', async () => {
        await renderMenu();

        composerRef.setText('/');

        await waitFor(() => {
            expect(screen.getByText('email-digest')).toBeInTheDocument();
        });

        fireEvent.keyDown(getComposerInput(), {key: 'Enter'});

        composerRef.setText('/log');

        await waitFor(() => {
            expect(screen.getByText('logLeads')).toBeInTheDocument();
        });

        fireEvent.keyDown(getComposerInput(), {key: 'Enter'});

        await waitFor(() => {
            expect(aiHubComposerStore.getState().selectedSkills).toEqual([
                {id: 'skill-1', name: 'email-digest'},
                {id: 'skill-3', name: 'logLeads'},
            ]);
        });
    });

    it('dismisses on Escape and stays shut until the composer text changes', async () => {
        await renderMenu();

        composerRef.setText('/');

        await waitFor(() => {
            expect(screen.getByText('email-digest')).toBeInTheDocument();
        });

        fireEvent.keyDown(getComposerInput(), {key: 'Escape'});

        await waitFor(() => {
            expect(screen.queryByText('email-digest')).not.toBeInTheDocument();
        });

        composerRef.setText('/e');

        await waitFor(() => {
            expect(screen.getByText('email-digest')).toBeInTheDocument();
        });
    });

    it('picks a skill on click', async () => {
        await renderMenu();

        composerRef.setText('/');

        await waitFor(() => {
            expect(screen.getByText('logLeads')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('logLeads'));

        await waitFor(() => {
            expect(aiHubComposerStore.getState().selectedSkills).toEqual([{id: 'skill-3', name: 'logLeads'}]);
        });
    });
});
