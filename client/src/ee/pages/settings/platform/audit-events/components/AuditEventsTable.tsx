import {Button} from '@/components/ui/button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {ChevronRightIcon} from 'lucide-react';

import {AuditEventItemType} from '../types';
import AuditEventOutcomeBadge from './AuditEventOutcomeBadge';

interface AuditEventsTablePropsI {
    auditEvents: AuditEventItemType[];
    onRowClick: (auditEvent: AuditEventItemType) => void;
}

const AuditEventsTable = ({auditEvents, onRowClick}: AuditEventsTablePropsI) => {
    if (auditEvents.length === 0) {
        return (
            <p className="p-8 text-center text-sm text-muted-foreground">No audit events match the current filters.</p>
        );
    }

    return (
        <Table>
            <TableHeader>
                <TableRow>
                    <TableHead>Date</TableHead>

                    <TableHead>Principal</TableHead>

                    <TableHead>Event Type</TableHead>

                    <TableHead>Result</TableHead>

                    <TableHead className="w-8" />
                </TableRow>
            </TableHeader>

            <TableBody>
                {auditEvents.map((auditEvent) => (
                    <TableRow className="cursor-pointer" key={auditEvent.id} onClick={() => onRowClick(auditEvent)}>
                        <TableCell className="font-mono text-xs whitespace-nowrap">
                            {new Date(auditEvent.eventDate).toLocaleString()}
                        </TableCell>

                        <TableCell>{auditEvent.principal}</TableCell>

                        <TableCell>{auditEvent.eventType}</TableCell>

                        <TableCell>
                            <AuditEventOutcomeBadge outcome={auditEvent.outcome} />
                        </TableCell>

                        <TableCell>
                            <Button size="icon" variant="ghost">
                                <ChevronRightIcon className="size-4" />
                            </Button>
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    );
};

export default AuditEventsTable;
