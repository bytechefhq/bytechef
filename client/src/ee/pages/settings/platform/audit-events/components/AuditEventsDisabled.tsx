import EmptyState from '@/components/EmptyState/EmptyState';
import {ScrollTextIcon} from 'lucide-react';

const AuditEventsDisabled = () => (
    <EmptyState
        description={
            <>
                Audit events are not being recorded. To start recording them, set the{' '}
                <code className="rounded bg-muted px-1 py-0.5 font-mono text-xs">BYTECHEF_AUDIT_ENABLED</code>{' '}
                environment variable (or{' '}
                <code className="rounded bg-muted px-1 py-0.5 font-mono text-xs">bytechef.audit.enabled</code> in the
                server configuration) to <code className="rounded bg-muted px-1 py-0.5 font-mono text-xs">true</code>{' '}
                and restart the server.
            </>
        }
        icon={<ScrollTextIcon />}
        title="Audit events are disabled"
    />
);

export default AuditEventsDisabled;
