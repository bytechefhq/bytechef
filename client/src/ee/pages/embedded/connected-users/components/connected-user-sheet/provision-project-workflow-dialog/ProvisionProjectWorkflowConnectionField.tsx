import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {CONNECTION_ORIGIN_BADGES} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connectionOriginBadges';
import {Connection} from '@/ee/shared/middleware/embedded/configuration';
import {PlusIcon} from 'lucide-react';

interface ProvisionProjectWorkflowConnectionFieldProps {
    componentTitle: string;
    connections: Connection[];
    id: string;
    onChange: (connectionId: string) => void;
    onNewConnection: () => void;
    value?: string;
}

const ProvisionProjectWorkflowConnectionField = ({
    componentTitle,
    connections,
    id,
    onChange,
    onNewConnection,
    value,
}: ProvisionProjectWorkflowConnectionFieldProps) => {
    if (connections.length === 0) {
        return (
            <div className="flex items-center justify-between gap-2">
                <p className="text-xs text-content-warning-primary">
                    {`No ${componentTitle} connection. Create one, or workflows that use ${componentTitle} will be provisioned disabled.`}
                </p>

                <Button
                    icon={<PlusIcon />}
                    label={`New ${componentTitle} connection`}
                    onClick={onNewConnection}
                    size="sm"
                    type="button"
                    variant="outline"
                />
            </div>
        );
    }

    return (
        <div className="space-y-1">
            <Label htmlFor={id}>{`${componentTitle} connection`}</Label>

            <div className="flex items-center gap-2">
                <Select onValueChange={onChange} value={value}>
                    <SelectTrigger className="w-full *:data-[slot=select-value]:w-full" id={id}>
                        <SelectValue />
                    </SelectTrigger>

                    <SelectContent>
                        {connections.map((connection) => {
                            const originBadge = connection.origin && CONNECTION_ORIGIN_BADGES[connection.origin];

                            return (
                                <SelectItem
                                    className="pr-2 *:[span]:first:hidden *:[span]:last:w-full"
                                    key={connection.id}
                                    value={String(connection.id)}
                                >
                                    <div className="flex w-full items-center justify-between gap-2">
                                        <span className="truncate">{connection.name}</span>

                                        {originBadge && (
                                            <Badge
                                                className="uppercase"
                                                label={originBadge.label}
                                                styleType="secondary-outline"
                                                weight="semibold"
                                            />
                                        )}
                                    </div>
                                </SelectItem>
                            );
                        })}
                    </SelectContent>
                </Select>

                <Button
                    aria-label={`New ${componentTitle} connection`}
                    icon={<PlusIcon />}
                    onClick={onNewConnection}
                    size="icon"
                    title="New connection"
                    type="button"
                    variant="outline"
                />
            </div>
        </div>
    );
};

export default ProvisionProjectWorkflowConnectionField;
