export interface UserI {
    id?: number;
    login?: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    activated?: boolean;
    langKey?: string;
    authorities?: string[];
    createdBy?: string;
    createdDate?: Date | null;
    lastModifiedBy?: string;
    lastModifiedDate?: Date | null;
    password?: string;
}
