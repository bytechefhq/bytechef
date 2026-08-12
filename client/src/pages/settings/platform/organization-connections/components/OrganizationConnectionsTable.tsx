import Button from '@/components/Button/Button';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {OrganizationConnection} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {PlugIcon, Trash2Icon} from 'lucide-react';
import {useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

interface OrganizationConnectionsTableProps {
    connections: OrganizationConnection[];
    onDeleteClick: (connection: OrganizationConnection) => void;
}

const OrganizationConnectionsTable = ({connections, onDeleteClick}: OrganizationConnectionsTableProps) => {
    // One list query for the whole table rather than a per-row lookup: the definitions are cached under a long stale
    // time and shared with every other surface that renders a component, so this is usually already resolved.
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    const componentsByName = useMemo(
        () => new Map((componentDefinitions ?? []).map((definition) => [definition.name, definition])),
        [componentDefinitions]
    );

    return (
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
                                index % 2 === 1 && 'bg-surface-neutral-secondary hover:bg-surface-neutral-secondary'
                            )}
                            key={connection.id}
                        >
                            <TableCell className="font-medium">{connection.name}</TableCell>

                            <TableCell>
                                <div className="flex items-center gap-2">
                                    {componentsByName.get(connection.componentName)?.icon ? (
                                        <InlineSVG
                                            className="size-5 flex-none"
                                            src={componentsByName.get(connection.componentName)!.icon!}
                                        />
                                    ) : (
                                        <PlugIcon className="size-5 flex-none text-muted-foreground" />
                                    )}

                                    {/* Falls back to the raw name: a connection can outlive the component that
                                    defined it, and showing nothing would read as a broken row. */}

                                    <span>
                                        {componentsByName.get(connection.componentName)?.title ??
                                            connection.componentName}
                                    </span>
                                </div>
                            </TableCell>

                            <TableCell>{connection.environmentId}</TableCell>

                            <TableCell>{connection.createdBy ?? '—'}</TableCell>

                            <TableCell className="whitespace-nowrap">{connection.lastModifiedDate ?? '—'}</TableCell>

                            <TableCell className="text-right">
                                <Button
                                    icon={<Trash2Icon className="size-4 text-destructive" />}
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
};

export default OrganizationConnectionsTable;
