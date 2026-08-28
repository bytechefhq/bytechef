import OwnerSelect from '@/ee/pages/embedded/shared/components/OwnerSelect';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';

interface EmbeddedDataTableI {
    baseName: string;
    description?: string | null;
    id: string;
    ownerId?: string | null;
}

interface EmbeddedDataTableListProps {
    connectedUsers: ConnectedUser[];
    dataTables: EmbeddedDataTableI[];
    onAssign: (dataTableId: string, ownerId: number | undefined) => void;
}

const EmbeddedDataTableList = ({connectedUsers, dataTables, onAssign}: EmbeddedDataTableListProps) => (
    <ul className="w-full divide-y divide-border/50 px-4 2xl:mx-auto 2xl:w-4/5">
        {dataTables.map((dataTable) => (
            <li className="flex items-center justify-between gap-4 py-4" key={dataTable.id}>
                <div className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-semibold">{dataTable.baseName}</span>

                    {dataTable.description && (
                        <span className="mt-1 block truncate text-xs text-content-neutral-secondary">
                            {dataTable.description}
                        </span>
                    )}
                </div>

                <OwnerSelect
                    connectedUsers={connectedUsers}
                    noOwnerLabel="Shared"
                    onChange={(ownerId) => onAssign(dataTable.id, ownerId)}
                    ownerId={dataTable.ownerId == null ? undefined : Number(dataTable.ownerId)}
                />
            </li>
        ))}
    </ul>
);

export default EmbeddedDataTableList;
