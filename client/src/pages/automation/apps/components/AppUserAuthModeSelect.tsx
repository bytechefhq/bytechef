import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {FormControl} from '@/components/ui/form';
import {APP_USER_AUTH_MODE_LABELS} from '@/pages/automation/apps/constants';
import {AppUserAuthMode} from '@/shared/middleware/graphql';

interface AppUserAuthModeSelectProps {
    onValueChange: (userAuthMode: AppUserAuthMode) => void;
    userAuthAvailable?: boolean;
    value: AppUserAuthMode;
}

const AppUserAuthModeSelect = ({onValueChange, userAuthAvailable, value}: AppUserAuthModeSelectProps) => (
    <>
        <Select onValueChange={(nextValue) => onValueChange(nextValue as AppUserAuthMode)} value={value}>
            <FormControl>
                <SelectTrigger>
                    <SelectValue />
                </SelectTrigger>
            </FormControl>

            <SelectContent>
                {Object.values(AppUserAuthMode).map((userAuthMode) => (
                    <SelectItem
                        disabled={userAuthAvailable === false && userAuthMode !== AppUserAuthMode.None}
                        key={userAuthMode}
                        value={userAuthMode}
                    >
                        {APP_USER_AUTH_MODE_LABELS[userAuthMode]}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>

        {userAuthAvailable === false && (
            <p className="text-sm text-content-neutral-secondary">
                Sign-in is unavailable: the authorization server is off, so the instance is created without sign-in.
            </p>
        )}
    </>
);

export default AppUserAuthModeSelect;
