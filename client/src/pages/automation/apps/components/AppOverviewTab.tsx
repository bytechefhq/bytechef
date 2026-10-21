import {HOSTING_TARGET_LABELS} from '@/pages/automation/apps/constants';
import {App, AppStatus} from '@/shared/middleware/graphql';

interface AppOverviewTabProps {
    app: App;
    ownerName: string;
}

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '-');

const AppOverviewTab = ({app, ownerName}: AppOverviewTabProps) => {
    const rows: Array<{label: string; value: string}> = [
        {label: 'Status', value: app.status === AppStatus.Enabled ? 'Enabled' : 'Disabled'},
        {label: 'Owner', value: ownerName},
        {label: 'URL', value: app.url || '-'},
        {label: 'Hosting target', value: HOSTING_TARGET_LABELS[app.hostingTarget]},
        {label: 'Key hint', value: `…${app.secretKeyHint}`},
        {label: 'Key last used', value: formatDate(app.secretKeyLastUsedDate)},
        {label: 'Last deployed', value: formatDate(app.lastDeployedDate)},
        {label: 'Created', value: formatDate(app.createdDate)},
        {label: 'Modified', value: formatDate(app.lastModifiedDate)},
    ];

    return (
        <dl className="grid grid-cols-[max-content_1fr] gap-x-8 gap-y-3 text-sm">
            {app.description && (
                <>
                    <dt className="text-content-neutral-secondary">Description</dt>

                    <dd>{app.description}</dd>
                </>
            )}

            {rows.map((row) => (
                <div className="contents" key={row.label}>
                    <dt className="text-content-neutral-secondary">{row.label}</dt>

                    <dd>{row.value}</dd>
                </div>
            ))}
        </dl>
    );
};

export default AppOverviewTab;
