import {PromotionChangeKind, PromotionResourceType, PromotionSchemaChangeKind} from '@/shared/middleware/graphql';
import {render, screen, within} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import EnvironmentPromotionChangesStep from '../EnvironmentPromotionChangesStep';
import {EnvironmentPromotionPreviewType} from '../hooks/useEnvironmentPromotionDialog';

const makePreview = (overrides: Partial<EnvironmentPromotionPreviewType> = {}): EnvironmentPromotionPreviewType => ({
    changes: [],
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

describe('EnvironmentPromotionChangesStep', () => {
    it('groups changes into Will create, Will update and Will delete', () => {
        render(
            <EnvironmentPromotionChangesStep
                isPreviewStale={false}
                preview={makePreview({
                    changes: [
                        {
                            assetType: 'MCP_TOOL',
                            destructive: false,
                            key: 'tool-new',
                            kind: PromotionChangeKind.Create,
                            name: 'New tool',
                            parentName: 'Slack',
                        },
                        {
                            assetType: 'WORKFLOW',
                            destructive: false,
                            key: 'workflow-1',
                            kind: PromotionChangeKind.Update,
                            name: 'Changed workflow',
                            parentName: null,
                        },
                        {
                            assetType: 'WORKFLOW_INPUT',
                            destructive: true,
                            key: 'input-1',
                            kind: PromotionChangeKind.Update,
                            name: 'accountId',
                            parentName: 'Notify workflow',
                        },
                        {
                            assetType: 'MCP_TOOL',
                            destructive: true,
                            key: 'tool-extra',
                            kind: PromotionChangeKind.Delete,
                            name: 'Extra tool',
                            parentName: 'Slack',
                        },
                    ],
                })}
                targetLabel="STAGING"
            />
        );

        const createGroup = screen.getByRole('group', {name: 'Will create'});
        const updateGroup = screen.getByRole('group', {name: 'Will update'});
        const deleteGroup = screen.getByRole('group', {name: 'Will delete'});

        expect(within(createGroup).getByText('New tool')).toBeInTheDocument();
        expect(within(createGroup).getByText('MCP tools')).toBeInTheDocument();
        expect(within(createGroup).getByText('Slack')).toBeInTheDocument();

        expect(within(updateGroup).getByText('Changed workflow')).not.toHaveClass('text-content-destructive-primary');
        expect(within(updateGroup).getByText('accountId')).toHaveClass('text-content-destructive-primary');

        expect(within(deleteGroup).getByText('Extra tool')).toHaveClass('text-content-destructive-primary');
        expect(within(deleteGroup).queryByText('New tool')).not.toBeInTheDocument();
    });

    it('derives column rows from schema changes', () => {
        render(
            <EnvironmentPromotionChangesStep
                isPreviewStale={false}
                preview={makePreview({
                    schemaChanges: [
                        {
                            columnName: 'notes',
                            destructive: false,
                            fromType: null,
                            kind: PromotionSchemaChangeKind.AddColumn,
                            tableName: 'orders',
                            toType: 'STRING',
                        },
                        {
                            columnName: 'amount',
                            destructive: true,
                            fromType: 'STRING',
                            kind: PromotionSchemaChangeKind.ChangeColumnType,
                            tableName: 'orders',
                            toType: 'NUMBER',
                        },
                        {
                            columnName: 'legacy',
                            destructive: true,
                            fromType: 'STRING',
                            kind: PromotionSchemaChangeKind.DropColumn,
                            tableName: 'orders',
                            toType: null,
                        },
                    ],
                })}
                targetLabel="STAGING"
            />
        );

        const createGroup = screen.getByRole('group', {name: 'Will create'});
        const updateGroup = screen.getByRole('group', {name: 'Will update'});
        const deleteGroup = screen.getByRole('group', {name: 'Will delete'});

        expect(within(createGroup).getByText('Columns')).toBeInTheDocument();
        expect(within(createGroup).getByText('orders')).toBeInTheDocument();
        expect(within(createGroup).getByText('notes')).not.toHaveClass('text-content-destructive-primary');

        expect(within(updateGroup).getByText('amount')).toHaveClass('text-content-destructive-primary');

        expect(within(deleteGroup).getByText('legacy')).toHaveClass('text-content-destructive-primary');

        expect(screen.getByText('Schema changes')).toBeInTheDocument();
    });

    it.each([
        ['AUTOMATION_CONFIGURATION', 'Automation configurations'],
        ['INTEGRATION_INSTANCE_CONFIGURATION', 'Integration instance configurations'],
        ['PROJECT_DEPLOYMENT', 'Project deployments'],
    ])('labels a top-level %s update as %s', (assetType, label) => {
        render(
            <EnvironmentPromotionChangesStep
                isPreviewStale={false}
                preview={makePreview({
                    changes: [
                        {
                            assetType,
                            destructive: false,
                            key: 'target-uuid',
                            kind: PromotionChangeKind.Update,
                            name: 'Orders',
                            parentName: null,
                        },
                    ],
                })}
                targetLabel="STAGING"
            />
        );

        const updateGroup = screen.getByRole('group', {name: 'Will update'});

        expect(within(updateGroup).getByText(label)).toBeInTheDocument();
        expect(within(updateGroup).getByText('Orders')).toBeInTheDocument();
        expect(screen.queryByText('Already up to date in STAGING')).not.toBeInTheDocument();
    });

    it('shows Already up to date when nothing changes', () => {
        render(
            <EnvironmentPromotionChangesStep
                isPreviewStale={false}
                preview={makePreview({
                    projects: [
                        {
                            projectId: 'project-1',
                            projectName: 'My Project',
                            sourceProjectVersion: 3,
                            targetProjectVersion: 3,
                        },
                    ],
                    warnings: ['contextPath differs from the target'],
                })}
                targetLabel="STAGING"
            />
        );

        expect(screen.getByText('Already up to date in STAGING')).toBeInTheDocument();
        expect(screen.queryByRole('group', {name: 'Will create'})).not.toBeInTheDocument();
        expect(screen.getByText(/v3 → v3/)).toBeInTheDocument();
        expect(screen.getByText('contextPath differs from the target')).toBeInTheDocument();
    });

    it('says promote creates the counterpart instead of Already up to date for a new target', () => {
        render(
            <EnvironmentPromotionChangesStep
                isPreviewStale={false}
                preview={makePreview({existingTargetId: null, existingTargetName: null})}
                targetLabel="STAGING"
            />
        );

        expect(screen.getByText('Promoting creates a new counterpart in STAGING.')).toBeInTheDocument();
        expect(screen.queryByText('Already up to date in STAGING')).not.toBeInTheDocument();
    });
});
