import Button from '@/components/Button/Button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {OrganizationConnection} from '@/shared/middleware/graphql';
import {Trash2Icon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface OrganizationConnectionsTableProps {
    connections: OrganizationConnection[];
    onDeleteClick: (connection: OrganizationConnection) => void;
}

const OrganizationConnectionsTable = ({connections, onDeleteClick}: OrganizationConnectionsTableProps) => (
    <div className="w-full px-2 3xl:mx-auto 3xl:w-4/5">
        <Table>
            <TableHeader>
                <TableRow>
                    <TableHead>Name</TableHead>

                    <TableHead>Component</TableHead>

                    <TableHead>Environment</TableHead>

                    <TableHead>Created By</TableHead>

                    <TableHead>Last Modified</TableHead>

                    <TableHead />
                </TableRow>
            </TableHeader>

            <TableBody>
                {connections.map((connection, index) => (
                    <TableRow
                        className={twMerge(
                            'border-b border-stroke-neutral-secondary hover:bg-transparent',
                            index % 2 === 1 && 'bg-gray-50 hover:bg-gray-50'
                        )}
                        key={connection.id}
                    >
                        <TableCell className="font-medium">{connection.name}</TableCell>

                        <TableCell>{connection.componentName}</TableCell>

                        <TableCell>{connection.environmentId}</TableCell>

                        <TableCell>{connection.createdBy ?? '—'}</TableCell>

                        <TableCell className="whitespace-nowrap">{connection.lastModifiedDate ?? '—'}</TableCell>

                        <TableCell className="text-right">
                            <Button
                                icon={<Trash2Icon className="size-4" />}
                                onClick={() => onDeleteClick(connection)}
                                size="icon"
                                variant="ghost"
                            />
                        </TableCell>
                    </TableRow>
                ))}
            </TableBody>
        </Table>
    </div>
);

export default OrganizationConnectionsTable;
