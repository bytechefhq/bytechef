/**
 * Downloads an agent skill archive as a .skill file via REST endpoint.
 * Uses a native browser download instead of GraphQL to avoid Base64 encoding overhead.
 */
export default async function downloadAiAgentSkill(id: string, skillName: string): Promise<void> {
    let response: Response;

    try {
        response = await fetch(`/api/ai/agent-skills/${encodeURIComponent(id)}/download`, {
            credentials: 'same-origin',
        });
    } catch (error) {
        throw new Error('Network error. Please check your connection and try again.', {cause: error});
    }

    if (response.status === 401 || response.status === 403) {
        throw new Error('Your session has expired. Please sign in again to download this skill.');
    }

    if (!response.ok) {
        throw new Error(`Download failed (HTTP ${response.status}). Please try again.`);
    }

    let blob: Blob;

    try {
        blob = await response.blob();
    } catch (error) {
        throw new Error('Download was interrupted. Please check your connection and try again.', {cause: error});
    }

    const url = URL.createObjectURL(blob);

    try {
        const anchor = document.createElement('a');

        anchor.href = url;
        anchor.download = `${skillName}.skill`;

        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
    } finally {
        URL.revokeObjectURL(url);
    }
}
