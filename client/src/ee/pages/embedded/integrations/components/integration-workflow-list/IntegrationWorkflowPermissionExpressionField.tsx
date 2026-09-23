import {Label} from '@/components/ui/label';
import {Textarea} from '@/components/ui/textarea';

interface IntegrationWorkflowPermissionExpressionFieldProps {
    onChange: (permissionExpression: string) => void;
    value?: string | null;
}

const IntegrationWorkflowPermissionExpressionField = ({
    onChange,
    value,
}: IntegrationWorkflowPermissionExpressionFieldProps) => (
    <fieldset className="space-y-2 border-0 p-0">
        <Label>Permission Expression</Label>

        <Textarea
            onChange={(event) => onChange(event.target.value)}
            placeholder="e.g. metadata['tier'] == 'gold'"
            rows={3}
            value={value ?? ''}
        />
    </fieldset>
);

export default IntegrationWorkflowPermissionExpressionField;
