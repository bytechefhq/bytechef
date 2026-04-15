import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';

import ConnectionVisibilityPicker, {deriveVisibilityState} from './ConnectionVisibilityPicker';

const MEMBERS = [
    {label: 'ana@example.com', userId: 8},
    {label: 'marko@example.com', userId: 9},
];

const renderPicker = (props: Partial<Parameters<typeof ConnectionVisibilityPicker>[0]> = {}) => {
    const onGrantedUserIdsChange = vi.fn();
    const onVisibilityChange = vi.fn();

    render(
        <ConnectionVisibilityPicker
            grantedUserIds={[]}
            onGrantedUserIdsChange={onGrantedUserIdsChange}
            onVisibilityChange={onVisibilityChange}
            visibility="WORKSPACE"
            workspaceMembers={MEMBERS}
            {...props}
        />
    );

    return {onGrantedUserIdsChange, onVisibilityChange};
};

describe('deriveVisibilityState', () => {
    it('reports SPECIFIC_PEOPLE for a private connection that has grants', () => {
        expect(deriveVisibilityState('PRIVATE', [8])).toBe('SPECIFIC_PEOPLE');
    });

    it('reports PRIVATE for a private connection with no grants', () => {
        expect(deriveVisibilityState('PRIVATE', [])).toBe('PRIVATE');
    });

    it('ignores grants once the connection is workspace-visible', () => {
        // Grants survive promotion so demoting restores the audience, but they are inert meanwhile — the state
        // shown must be the reach, not the leftover rows.
        expect(deriveVisibilityState('WORKSPACE', [8])).toBe('WORKSPACE');
    });
});

describe('ConnectionVisibilityPicker', () => {
    it('preselects Shared with workspace for a new connection', () => {
        renderPicker();

        expect(screen.getByRole('radio', {name: /shared with workspace/i})).toBeChecked();
    });

    it('shows the people picker only when Specific people is selected', async () => {
        const user = userEvent.setup();

        renderPicker({grantedUserIds: [], visibility: 'PRIVATE'});

        expect(screen.queryByLabelText(/add person/i)).not.toBeInTheDocument();

        await user.click(screen.getByRole('radio', {name: /specific people/i}));

        expect(screen.getByLabelText(/add person/i)).toBeInTheDocument();
    });

    it('reports PRIVATE when Specific people is chosen', async () => {
        const user = userEvent.setup();

        const {onVisibilityChange} = renderPicker();

        await user.click(screen.getByRole('radio', {name: /specific people/i}));

        // Specific people is not a fourth stored value; it is PRIVATE plus grants.
        expect(onVisibilityChange).toHaveBeenCalledWith('PRIVATE');
    });

    it('renders Specific people as selected when grants exist', () => {
        renderPicker({grantedUserIds: [8], visibility: 'PRIVATE'});

        expect(screen.getByRole('radio', {name: /specific people/i})).toBeChecked();
    });

    it('lists the granted people by name', () => {
        renderPicker({grantedUserIds: [8], visibility: 'PRIVATE'});

        expect(screen.getByText('ana@example.com')).toBeInTheDocument();
    });

    it('removes a person when their chip is dismissed', async () => {
        const user = userEvent.setup();

        const {onGrantedUserIdsChange} = renderPicker({grantedUserIds: [8, 9], visibility: 'PRIVATE'});

        await user.click(screen.getByRole('button', {name: /remove ana@example.com/i}));

        expect(onGrantedUserIdsChange).toHaveBeenCalledWith([9]);
    });

    it('keeps Specific people selected after switching to it from Shared, before anyone is named', async () => {
        // Regression: "Specific people" stores as PRIVATE with no grants yet, which derives back to PRIVATE. Without
        // holding the intent, the radio snapped to Private the instant it was clicked and the people picker was
        // unreachable — you could never name the first person.
        const user = userEvent.setup();

        const ControlledHarness = () => {
            const [visibility, setVisibility] = useState<'ORGANIZATION' | 'PRIVATE' | 'WORKSPACE'>('WORKSPACE');
            const [grantedUserIds, setGrantedUserIds] = useState<number[]>([]);

            return (
                <ConnectionVisibilityPicker
                    grantedUserIds={grantedUserIds}
                    onGrantedUserIdsChange={setGrantedUserIds}
                    onVisibilityChange={setVisibility}
                    visibility={visibility}
                    workspaceMembers={MEMBERS}
                />
            );
        };

        render(<ControlledHarness />);

        await user.click(screen.getByRole('radio', {name: /specific people/i}));

        expect(screen.getByRole('radio', {name: /specific people/i})).toBeChecked();
        expect(screen.getByLabelText(/add person/i)).toBeInTheDocument();
    });

    it('hides the Organization option for non-admins', () => {
        renderPicker({isAdmin: false, showOrganizationOption: true});

        expect(screen.queryByRole('radio', {name: /organization/i})).not.toBeInTheDocument();
    });

    it('hides the Organization option for admins where it is unsupported', () => {
        renderPicker({isAdmin: true, showOrganizationOption: false});

        expect(screen.queryByRole('radio', {name: /organization/i})).not.toBeInTheDocument();
    });

    it('offers the Organization option to an admin where it is supported', () => {
        renderPicker({isAdmin: true, showOrganizationOption: true});

        expect(screen.getByRole('radio', {name: /organization/i})).toBeInTheDocument();
    });
});
