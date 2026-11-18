import Switch from '@/components/Switch/Switch';
import {ConnectedUserProjectWorkflow} from '@bytechef/embedded-api';

interface AutomationAiAgentsSwitchProps {
    automation: ConnectedUserProjectWorkflow;
    onChange: (enabled: boolean) => void;
}

const AutomationAiAgentsSwitch = ({automation, onChange}: AutomationAiAgentsSwitchProps) => (
    <Switch
        aria-label="Available to AI agents"
        checked={automation.mcpEnabled === true}
        label="Available to AI agents"
        onCheckedChange={onChange}
        variant="small"
    />
);

export default AutomationAiAgentsSwitch;
