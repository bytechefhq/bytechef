import {APP_STATUS_LABELS} from '@/pages/automation/apps/constants';
import {App} from '@/shared/middleware/graphql';

interface AppOverviewTabProps {
    app: Pick<App, 'createdDate' | 'description' | 'foreignInstances' | 'instances' | 'lastModifiedDate' | 'status'>;
    ownerName: string;
}

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '-');

const AppOverviewTab = ({app, ownerName}: AppOverviewTabProps) => {
    const rows: Array<{label: string; value: string}> = [
        {label: 'Status', value: APP_STATUS_LABELS[app.status]},
        {label: 'Owner', value: ownerName},
        {label: 'Instances', value: String(app.instances.length + app.foreignInstances.length)},
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
