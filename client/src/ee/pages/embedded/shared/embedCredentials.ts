export interface EmbedCredentialsI {
    environment: string | null;
    jwtToken: string | null;
}

const EMPTY_EMBED_CREDENTIALS: EmbedCredentialsI = {environment: null, jwtToken: null};

let embedCredentials: EmbedCredentialsI = EMPTY_EMBED_CREDENTIALS;

export const getEmbedCredentials = (): EmbedCredentialsI => embedCredentials;

export const setEmbedCredentials = (credentials: EmbedCredentialsI): void => {
    embedCredentials = {...credentials};
};

export const resetEmbedCredentials = (): void => {
    embedCredentials = EMPTY_EMBED_CREDENTIALS;
};
