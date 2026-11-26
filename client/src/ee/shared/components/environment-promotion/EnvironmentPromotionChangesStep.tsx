import {Alert, AlertDescription} from '@/components/ui/alert';
import {PromotionChangeKind} from '@/shared/middleware/graphql';
import {AlertTriangleIcon, InfoIcon} from 'lucide-react';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

import EnvironmentPromotionSchemaChanges from './EnvironmentPromotionSchemaChanges';
import {
    EnvironmentPromotionPreviewType,
    PROMOTION_PREVIEW_STALE_MESSAGE,
    PromotionChangeRowI,
    getPromotionChangeRows,
} from './hooks/useEnvironmentPromotionDialog';

const ASSET_TYPE_LABELS: Record<string, string> = {
    A2A_PROJECT: 'A2A projects',
    API_COLLECTION: 'API collections',
    AUTOMATION_CONFIGURATION: 'Automation configurations',
    COLUMN: 'Columns',
    CONFIGURATION_WORKFLOW: 'Configuration workflows',
    DATA_TABLE: 'Data tables',
    DEPLOYMENT_WORKFLOW: 'Deployment workflows',
    ENDPOINT: 'Endpoints',
    EXPOSED_WORKFLOW: 'Exposed workflows',
    INTEGRATION_INSTANCE_CONFIGURATION: 'Integration instance configurations',
    MCP_COMPONENT: 'MCP components',
    MCP_PROJECT: 'MCP projects',
    MCP_TOOL: 'MCP tools',
    PROJECT_DEPLOYMENT: 'Project deployments',
    WORKFLOW: 'Workflows',
    WORKFLOW_INPUT: 'Workflow inputs',
};

const CHANGE_KIND_GROUPS = [
    {kind: PromotionChangeKind.Create, label: 'Will create'},
    {kind: PromotionChangeKind.Update, label: 'Will update'},
    {kind: PromotionChangeKind.Delete, label: 'Will delete'},
];

interface ParentGroupI {
    parentName: string | null;
    rows: PromotionChangeRowI[];
}

interface AssetTypeGroupI {
    assetType: string;
    parentGroups: ParentGroupI[];
}

interface ChangeKindGroupI {
    assetTypeGroups: AssetTypeGroupI[];
    kind: PromotionChangeKind;
    label: string;
}

const groupRowsByAssetTypeAndParent = (rows: PromotionChangeRowI[]): AssetTypeGroupI[] =>
    rows.reduce<AssetTypeGroupI[]>((assetTypeGroups, row) => {
        let assetTypeGroup = assetTypeGroups.find((group) => group.assetType === row.assetType);

        if (!assetTypeGroup) {
            assetTypeGroup = {assetType: row.assetType, parentGroups: []};

            assetTypeGroups.push(assetTypeGroup);
        }

        let parentGroup = assetTypeGroup.parentGroups.find((group) => group.parentName === row.parentName);

        if (!parentGroup) {
            parentGroup = {parentName: row.parentName, rows: []};

            assetTypeGroup.parentGroups.push(parentGroup);
        }

        parentGroup.rows.push(row);

        return assetTypeGroups;
    }, []);

export interface EnvironmentPromotionChangesStepProps {
    isPreviewStale: boolean;
    preview: EnvironmentPromotionPreviewType;
    targetLabel: string;
}

const EnvironmentPromotionChangesStep = ({
    isPreviewStale,
    preview,
    targetLabel,
}: EnvironmentPromotionChangesStepProps) => {
    const changeKindGroups = useMemo<ChangeKindGroupI[]>(() => {
        const changeRows = getPromotionChangeRows(preview);

        return CHANGE_KIND_GROUPS.map(({kind, label}) => ({
            assetTypeGroups: groupRowsByAssetTypeAndParent(changeRows.filter((row) => row.kind === kind)),
            kind,
            label,
        })).filter((changeKindGroup) => changeKindGroup.assetTypeGroups.length > 0);
    }, [preview]);

    return (
        <div className="space-y-4">
            {isPreviewStale && (
                <Alert variant="warning">
                    <AlertTriangleIcon />

                    <AlertDescription>{PROMOTION_PREVIEW_STALE_MESSAGE}</AlertDescription>
                </Alert>
            )}

            {preview.existingTargetId == null && (
                <p className="text-sm">{`Promoting creates a new counterpart in ${targetLabel}.`}</p>
            )}

            {changeKindGroups.length === 0 && preview.existingTargetId != null && (
                <div className="flex items-center gap-2 text-sm text-content-neutral-secondary">
                    <InfoIcon className="size-4" />

                    <span>{`Already up to date in ${targetLabel}`}</span>
                </div>
            )}

            {changeKindGroups.map((changeKindGroup) => (
                <fieldset className="space-y-2 border-0" key={changeKindGroup.kind}>
                    <legend className="text-sm font-medium">{changeKindGroup.label}</legend>

                    {changeKindGroup.assetTypeGroups.map((assetTypeGroup) => (
                        <div className="space-y-1 pl-2" key={assetTypeGroup.assetType}>
                            <span className="text-xs font-medium text-content-neutral-secondary uppercase">
                                {ASSET_TYPE_LABELS[assetTypeGroup.assetType] || assetTypeGroup.assetType}
                            </span>

                            {assetTypeGroup.parentGroups.map((parentGroup) => (
                                <div className="space-y-1 pl-2" key={parentGroup.parentName || ''}>
                                    {parentGroup.parentName && (
                                        <span className="text-sm text-content-neutral-secondary">
                                            {parentGroup.parentName}
                                        </span>
                                    )}

                                    <ul className="space-y-1 pl-2">
                                        {parentGroup.rows.map((row) => (
                                            <li
                                                className={twMerge(
                                                    'text-sm',
                                                    row.destructive && 'text-content-destructive-primary'
                                                )}
                                                key={`${row.assetType}|${row.parentName || ''}|${row.key}|${row.kind}`}
                                            >
                                                {row.name}
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            ))}
                        </div>
                    ))}
                </fieldset>
            ))}

            {preview.projects.length > 0 && (
                <ul className="space-y-1 text-sm text-content-neutral-secondary">
                    {preview.projects.map((project) => (
                        <li key={project.projectId}>
                            {`${project.projectName}: v${project.sourceProjectVersion} → ${
                                project.targetProjectVersion != null ? `v${project.targetProjectVersion}` : 'new'
                            }`}
                        </li>
                    ))}
                </ul>
            )}

            {preview.schemaChanges.length > 0 && (
                <EnvironmentPromotionSchemaChanges schemaChanges={preview.schemaChanges} />
            )}

            {preview.warnings.map((warning) => (
                <Alert key={warning} variant="warning">
                    <AlertTriangleIcon />

                    <AlertDescription>{warning}</AlertDescription>
                </Alert>
            ))}
        </div>
    );
};

export default EnvironmentPromotionChangesStep;
