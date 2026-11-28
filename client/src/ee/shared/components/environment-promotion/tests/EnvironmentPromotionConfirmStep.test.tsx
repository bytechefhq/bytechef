import {PromotionChangeKind, PromotionResourceType} from '@/shared/middleware/graphql';
import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import EnvironmentPromotionConfirmStep from '../EnvironmentPromotionConfirmStep';
import {EnvironmentPromotionPreviewType} from '../hooks/useEnvironmentPromotionDialog';

const makePreview = (overrides: Partial<EnvironmentPromotionPreviewType> = {}): EnvironmentPromotionPreviewType => ({
    changes: [
        {
            assetType: 'MCP_TOOL',
            destructive: false,
            key: 'tool-new',
            kind: PromotionChangeKind.Create,
            name: 'New tool',
            parentName: null,
        },
        {
            assetType: 'MCP_TOOL',
            destructive: false,
            key: 'tool-changed',
            kind: PromotionChangeKind.Update,
            name: 'Changed tool',
            parentName: null,
        },
    ],
    connections: [],
    destructiveChangesHash: 'hash-1',
    existingTargetId: 'existing-1',
    existingTargetName: 'Existing Server',
    projects: [],
    resourceType: PromotionResourceType.McpServer,
    schemaChanges: [],
    sourceEnvironmentId: '0',
    sourceId: 'source-1',
    targetEnvironmentId: '1',
    warnings: [],
    ...overrides,
});

describe('EnvironmentPromotionConfirmStep', () => {
    it('shows the counts and the acknowledgement only when destructive changes exist', async () => {
        const onDestructiveAcknowledgedChange = vi.fn();

        const {unmount} = render(
            <EnvironmentPromotionConfirmStep
                destructiveChangeCount={0}
                isDestructiveAcknowledged={false}
                onDestructiveAcknowledgedChange={onDestructiveAcknowledgedChange}
                preview={makePreview()}
                targetLabel="STAGING"
            />
        );

        expect(screen.getByText('1 to create')).toBeInTheDocument();
        expect(screen.getByText('1 to update')).toBeInTheDocument();
        expect(screen.getByText('0 to delete')).toBeInTheDocument();
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();

        unmount();

        render(
            <EnvironmentPromotionConfirmStep
                destructiveChangeCount={1}
                isDestructiveAcknowledged={false}
                onDestructiveAcknowledgedChange={onDestructiveAcknowledgedChange}
                preview={makePreview({
                    changes: [
                        {
                            assetType: 'MCP_TOOL',
                            destructive: true,
                            key: 'tool-extra',
                            kind: PromotionChangeKind.Delete,
                            name: 'Extra tool',
                            parentName: null,
                        },
                    ],
                })}
                targetLabel="STAGING"
            />
        );

        expect(screen.getByText('1 to delete')).toBeInTheDocument();

        const checkbox = screen.getByRole('checkbox', {
            name: 'I understand 1 change(s) delete or overwrite data in STAGING',
        });

        expect(checkbox).not.toBeChecked();

        await userEvent.setup().click(checkbox);

        expect(onDestructiveAcknowledgedChange).toHaveBeenCalledWith(true);
    });
});
