import Switch from '@/components/Switch/Switch';
import {twMerge} from 'tailwind-merge';

interface ModeSwitchPropsI {
    build: boolean;
    className?: string;
    onBuildChange: (build: boolean) => void;
}

const ModeSwitch = ({build, className, onBuildChange}: ModeSwitchPropsI) => (
    <div className={twMerge('flex items-center gap-1.5', className)}>
        <span className="text-xs font-medium text-muted-foreground">Build</span>

        <Switch
            aria-label={build ? 'Build mode on' : 'Build mode off (Ask)'}
            checked={build}
            className="data-[state=checked]:bg-surface-success-primary"
            onCheckedChange={onBuildChange}
        />
    </div>
);

export default ModeSwitch;
