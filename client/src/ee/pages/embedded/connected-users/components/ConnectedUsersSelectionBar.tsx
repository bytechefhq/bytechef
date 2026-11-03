import Button from '@/components/Button/Button';

interface ConnectedUsersSelectionBarProps {
    count: number;
    onClear: () => void;
    onUpgradeAutomation: () => void;
    onUpgradeIntegration: () => void;
}

const ConnectedUsersSelectionBar = ({
    count,
    onClear,
    onUpgradeAutomation,
    onUpgradeIntegration,
}: ConnectedUsersSelectionBarProps) => {
    if (count === 0) {
        return null;
    }

    return (
        <div className="flex items-center gap-2 px-4 pb-2">
            <span className="text-sm font-medium whitespace-nowrap">{`${count} selected`}</span>

            <Button label="Upgrade integration" onClick={onUpgradeIntegration} size="sm" variant="outline" />

            <Button label="Upgrade automation" onClick={onUpgradeAutomation} size="sm" variant="outline" />

            <Button label="Clear" onClick={onClear} size="sm" variant="ghost" />
        </div>
    );
};

export default ConnectedUsersSelectionBar;
