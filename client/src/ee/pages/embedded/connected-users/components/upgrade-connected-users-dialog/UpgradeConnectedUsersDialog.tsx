import AutomationUpgradeConnectedUsersDialog from '@/ee/pages/embedded/connected-users/components/upgrade-connected-users-dialog/AutomationUpgradeConnectedUsersDialog';
import IntegrationUpgradeConnectedUsersDialog from '@/ee/pages/embedded/connected-users/components/upgrade-connected-users-dialog/IntegrationUpgradeConnectedUsersDialog';
import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';

interface UpgradeConnectedUsersDialogProps {
    connectedUsers: ConnectedUser[];
    kind: 'automation' | 'integration';
    onClose: () => void;
    onUpgraded: () => void;
}

const UpgradeConnectedUsersDialog = ({kind, ...props}: UpgradeConnectedUsersDialogProps) =>
    kind === 'automation' ? (
        <AutomationUpgradeConnectedUsersDialog {...props} />
    ) : (
        <IntegrationUpgradeConnectedUsersDialog {...props} />
    );

export default UpgradeConnectedUsersDialog;
