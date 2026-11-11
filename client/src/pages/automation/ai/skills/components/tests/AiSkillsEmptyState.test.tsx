import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import AiSkillsEmptyState from '../AiSkillsEmptyState';

vi.mock('@/pages/automation/ai/skills/components/AiSkillsCreateDropdown', () => ({
    default: () => <button type="button">Create Skill</button>,
}));

describe('AiSkillsEmptyState', () => {
    it('links to the skills documentation in a new tab', () => {
        render(<AiSkillsEmptyState />);

        const documentationLink = screen.getByRole('link', {name: 'How skills work'});

        expect(documentationLink).toHaveAttribute('href', 'https://docs.bytechef.io/platform/automation/ai/skills');
        expect(documentationLink).toHaveAttribute('target', '_blank');
        expect(screen.queryByRole('button', {name: 'Read more'})).not.toBeInTheDocument();
    });
});
