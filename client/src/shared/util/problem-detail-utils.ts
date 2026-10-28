export const ACCESS_DENIED_ERROR_CODE = 'ACCESS_DENIED';

export interface ProblemDetailI {
    detail?: string;
    errorCode?: string;
    title?: string;
}

export async function readProblemDetail(response: Response): Promise<ProblemDetailI | null> {
    try {
        const clonedResponse = response.clone();

        return await clonedResponse.json();
    } catch {
        return null;
    }
}

export function isAccessDeniedProblemDetail(
    problemDetail: ProblemDetailI | null | undefined
): problemDetail is ProblemDetailI {
    return problemDetail?.errorCode === ACCESS_DENIED_ERROR_CODE;
}
