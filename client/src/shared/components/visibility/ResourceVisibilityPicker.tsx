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
    onGrantedUserIdsChange: (userIds: number[]) => void;
    onVisibilityChange: (visibility: ResourceVisibilityValueType) => void;
    showSpecificPeopleOption?: boolean;
    visibility: ResourceVisibilityValueType;
    workspaceMembers?: WorkspaceMemberI[];
}

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
    onGrantedUserIdsChange,
    onVisibilityChange,
    showSpecificPeopleOption = true,
    visibility,
    workspaceMembers = [],
}: ResourceVisibilityPickerPropsI) => {
    const [specificPeopleIntent, setSpecificPeopleIntent] = useState(false);

    const state =
        specificPeopleIntent && visibility === 'PRIVATE'
            ? 'SPECIFIC_PEOPLE'
            : deriveVisibilityState(visibility, grantedUserIds);

    const handleStateChange = (nextState: string) => {
        if (nextState === 'SPECIFIC_PEOPLE') {
            setSpecificPeopleIntent(true);

            onVisibilityChange('PRIVATE');

            return;
        }

        setSpecificPeopleIntent(false);

        if (nextState === 'PRIVATE' && grantedUserIds.length > 0) {
            onGrantedUserIdsChange([]);
        }

        if (nextState === visibility) {
            return;
        }

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

                {showSpecificPeopleOption && (
                    <div className="flex items-center space-x-2">
                        <RadioGroupItem id="visibility-specific-people" value="SPECIFIC_PEOPLE" />

                        <Label className="font-normal" htmlFor="visibility-specific-people">
                            Specific people
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
