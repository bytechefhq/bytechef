import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {APP_INSTANCE_STATUS_LABELS} from '@/pages/automation/apps/constants';
import {toEnvironmentName} from '@/shared/constants';
import {AppInstance} from '@/shared/middleware/graphql';

interface AppInstanceSelectProps {
    appInstances: Array<Pick<AppInstance, 'environmentId' | 'id' | 'name' | 'status'>>;
    onValueChange: (appInstanceId: string) => void;
    value?: string;
}

const AppInstanceSelect = ({appInstances, onValueChange, value}: AppInstanceSelectProps) => (
    <Select onValueChange={onValueChange} value={value}>
        <SelectTrigger aria-label="Instance" className="w-72">
            <SelectValue placeholder="Choose an instance" />
        </SelectTrigger>

        <SelectContent>
            {appInstances.map((appInstance) => (
                <SelectItem key={appInstance.id} value={appInstance.id}>
                    {`${appInstance.name} (${toEnvironmentName(Number(appInstance.environmentId))}, ${APP_INSTANCE_STATUS_LABELS[appInstance.status]})`}
                </SelectItem>
            ))}
        </SelectContent>
    </Select>
);

export default AppInstanceSelect;
