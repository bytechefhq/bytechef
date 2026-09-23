import {ResponseError} from '@/ee/shared/middleware/embedded/public';
import {toast} from 'sonner';

interface AutomationConflictBodyI {
    missingConnectionComponentName?: string;
    missingInputName?: string;
}

interface ComponentTitleI {
    name?: string;
    title?: string;
}

interface ReportSetEnabledErrorOptionsI {
    components?: ComponentTitleI[];
    label: string;
    onMissingInput?: (inputName: string) => void;
}

const readConflictBody = async (error: ResponseError): Promise<AutomationConflictBodyI | undefined> => {
    try {
        return (await error.response.clone().json()) as AutomationConflictBodyI;
    } catch {
        return undefined;
    }
};

const isReportedByFetchInterceptor = (error: unknown) =>
    error instanceof ResponseError && error.response.status !== 409;

export const reportSetEnabledError = async (
    error: unknown,
    {components, label, onMissingInput}: ReportSetEnabledErrorOptionsI
): Promise<void> => {
    if (isReportedByFetchInterceptor(error)) {
        return;
    }

    const title = `Could not enable "${label}"`;

    const conflictBody = error instanceof ResponseError ? await readConflictBody(error) : undefined;

    if (conflictBody?.missingConnectionComponentName) {
        const componentName = conflictBody.missingConnectionComponentName;
        const componentTitle =
            components?.find((component) => component.name === componentName)?.title || componentName;

        toast.error(title, {description: `Connect a ${componentTitle} account first, then enable it again.`});

        return;
    }

    if (conflictBody?.missingInputName) {
        toast.error(title, {description: `The "${conflictBody.missingInputName}" setting needs a value first.`});

        onMissingInput?.(conflictBody.missingInputName);

        return;
    }

    toast.error(title, {description: error instanceof Error && error.message ? error.message : undefined});
};

export const reportRemoveError = (error: unknown, label: string): void => {
    if (isReportedByFetchInterceptor(error)) {
        return;
    }

    toast.error(`Could not remove "${label}"`, {
        description: error instanceof Error && error.message ? error.message : undefined,
    });
};
