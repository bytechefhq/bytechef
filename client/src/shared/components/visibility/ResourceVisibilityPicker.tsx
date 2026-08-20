import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Label} from '@/components/ui/label';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {XIcon} from 'lucide-react';
import {useState} from 'react';

export type ResourceVisibilityValueType = 'ORGANIZATION' | 'PRIVATE' | 'WORKSPACE';

export type ResourceVisibilityStateType = ResourceVisibilityValueType | 'SPECIFIC_PEOPLE';

export interface WorkspaceMemberI {
    label: string;
    userId: number;
}

interface ResourceVisibilityPickerPropsI {
    grantedUserIds: number[];
    isAdmin?: boolean;
    onGrantedUserIdsChange: (userIds: number[]) => void;
    onVisibilityChange: (visibility: ResourceVisibilityValueType) => void;
    showOrganizationOption?: boolean;
    showSpecificPeopleOption?: boolean;
    visibility: ResourceVisibilityValueType;
    workspaceMembers?: WorkspaceMemberI[];
}

/**
 * The four states are three stored values plus the presence of grants: "Specific people" IS PRIVATE, distinguished
 * only by having grant rows. Keeping one stored state machine means the server never has to reconcile a fourth
 * enum value that would be indistinguishable from PRIVATE the moment its last grant was revoked.
 */
export const deriveVisibilityState = (
    visibility: ResourceVisibilityValueType,
    grantedUserIds: number[]
): ResourceVisibilityStateType => {
    if (visibility === 'PRIVATE' && grantedUserIds.length > 0) {
        return 'SPECIFIC_PEOPLE';
    }

    return visibility;
};

const ResourceVisibilityPicker = ({
    grantedUserIds,
    isAdmin = false,
    onGrantedUserIdsChange,
    onVisibilityChange,
    showOrganizationOption = false,
    showSpecificPeopleOption = true,
    visibility,
    workspaceMembers = [],
}: ResourceVisibilityPickerPropsI) => {
    // "Specific people" cannot be derived from props alone. It stores as PRIVATE, so a user who selects it before
    // naming anyone would see the radio snap straight back to Private with no way to reach the people picker. This
    // holds the intent until the first grant exists, after which the derived state carries it.
    const [specificPeopleIntent, setSpecificPeopleIntent] = useState(false);

    const state =
        specificPeopleIntent && visibility === 'PRIVATE'
            ? 'SPECIFIC_PEOPLE'
            : deriveVisibilityState(visibility, grantedUserIds);

    const handleStateChange = (nextState: string) => {
        if (nextState === 'SPECIFIC_PEOPLE') {
            // Withhold from the workspace, then let the picker below name who gets it back. Existing grants are
            // preserved so toggling away and back does not silently empty the audience.
            setSpecificPeopleIntent(true);

            onVisibilityChange('PRIVATE');

            return;
        }

        setSpecificPeopleIntent(false);

        // Leaving PRIVATE makes any grants inert rather than deleting them, so demoting again restores the same
        // audience. The server keeps the rows for the same reason.
        onVisibilityChange(nextState as ResourceVisibilityValueType);
    };

    const ungrantedMembers = workspaceMembers.filter((member) => !grantedUserIds.includes(member.userId));

    const memberLabel = (userId: number) =>
        workspaceMembers.find((member) => member.userId === userId)?.label ?? `User ${userId}`;

    return (
        <div className="space-y-3">
            <RadioGroup onValueChange={handleStateChange} value={state}>
                <div className="flex items-center space-x-2">
                    <RadioGroupItem id="visibility-workspace" value="WORKSPACE" />

                    <Label className="font-normal" htmlFor="visibility-workspace">
                        Shared with workspace
                    </Label>
                </div>

                <div className="flex items-center space-x-2">
                    <RadioGroupItem id="visibility-private" value="PRIVATE" />

                    <Label className="font-normal" htmlFor="visibility-private">
                        Private
                    </Label>
                </div>

                {/* Naming people requires grant rows, which require the resource to exist. A create dialog has no
                    id to grant against, so it must not OFFER the state - selecting it there would silently reduce
                    the user's stated intent to Private and reveal an empty people picker. Defaults to shown, so
                    every already-shipped caller keeps its four states. */}

                {showSpecificPeopleOption && (
                    <div className="flex items-center space-x-2">
                        <RadioGroupItem id="visibility-specific-people" value="SPECIFIC_PEOPLE" />

                        <Label className="font-normal" htmlFor="visibility-specific-people">
                            Specific people
                        </Label>
                    </div>
                )}

                {showOrganizationOption && isAdmin && (
                    <div className="flex items-center space-x-2">
                        <RadioGroupItem id="visibility-organization" value="ORGANIZATION" />

                        <Label className="font-normal" htmlFor="visibility-organization">
                            Organization
                        </Label>
                    </div>
                )}
            </RadioGroup>

            {showSpecificPeopleOption && state === 'SPECIFIC_PEOPLE' && (
                <div className="space-y-2 pl-6">
                    <div className="flex flex-wrap gap-1">
                        {grantedUserIds.map((userId) => (
                            <Badge className="gap-1" key={userId} styleType="secondary-outline">
                                {memberLabel(userId)}

                                <Button
                                    aria-label={`Remove ${memberLabel(userId)}`}
                                    className="size-4 p-0"
                                    icon={<XIcon className="size-3" />}
                                    onClick={() => onGrantedUserIdsChange(grantedUserIds.filter((id) => id !== userId))}
                                    size="icon"
                                    type="button"
                                    variant="ghost"
                                />
                            </Badge>
                        ))}
                    </div>

                    <Select
                        onValueChange={(value) => onGrantedUserIdsChange([...grantedUserIds, Number(value)])}
                        value=""
                    >
                        <SelectTrigger aria-label="Add person" className="w-full">
                            <SelectValue placeholder="Add person..." />
                        </SelectTrigger>

                        <SelectContent>
                            {ungrantedMembers.map((member) => (
                                <SelectItem key={member.userId} value={String(member.userId)}>
                                    {member.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
            )}
        </div>
    );
};

export default ResourceVisibilityPicker;
