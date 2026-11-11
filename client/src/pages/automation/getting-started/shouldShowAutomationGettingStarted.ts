interface ShouldShowAutomationGettingStartedProps {
    canEdit: boolean;
    completed?: boolean;
    development: boolean;
}

export default function shouldShowAutomationGettingStarted({
    canEdit,
    completed,
    development,
}: ShouldShowAutomationGettingStartedProps): boolean {
    return canEdit && development && completed === false;
}
