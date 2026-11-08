import {Label} from '@/components/ui/label';
import {Slider} from '@/components/ui/slider';

interface PlaygroundParametersPanelProps {
    maxTokens: number;
    onMaxTokensChange: (value: number) => void;
    onTemperatureChange: (value: number) => void;
    onTopPChange: (value: number) => void;
    temperature: number;
    topP: number;
}

const PlaygroundParametersPanel = ({
    maxTokens,
    onMaxTokensChange,
    onTemperatureChange,
    onTopPChange,
    temperature,
    topP,
}: PlaygroundParametersPanelProps) => {
    return (
        <fieldset className="space-y-5 border-0 p-0">
            <legend className="mb-3 text-sm font-semibold">Parameters</legend>

            <div className="space-y-2">
                <div className="flex items-center justify-between">
                    <Label htmlFor="temperature">Temperature</Label>

                    <span className="text-xs text-muted-foreground">{temperature.toFixed(2)}</span>
                </div>

                <Slider
                    id="temperature"
                    max={2}
                    min={0}
                    onValueChange={(values) => onTemperatureChange(values[0])}
                    step={0.01}
                    value={[temperature]}
                />
            </div>

            <div className="space-y-2">
                <div className="flex items-center justify-between">
                    <Label htmlFor="maxTokens">Max Tokens</Label>

                    <span className="text-xs text-muted-foreground">{maxTokens}</span>
                </div>

                <Slider
                    id="maxTokens"
                    max={16384}
                    min={1}
                    onValueChange={(values) => onMaxTokensChange(values[0])}
                    step={1}
                    value={[maxTokens]}
                />
            </div>

            <div className="space-y-2">
                <div className="flex items-center justify-between">
                    <Label htmlFor="topP">Top P</Label>

                    <span className="text-xs text-muted-foreground">{topP.toFixed(2)}</span>
                </div>

                <Slider
                    id="topP"
                    max={1}
                    min={0}
                    onValueChange={(values) => onTopPChange(values[0])}
                    step={0.01}
                    value={[topP]}
                />
            </div>
        </fieldset>
    );
};

export default PlaygroundParametersPanel;
