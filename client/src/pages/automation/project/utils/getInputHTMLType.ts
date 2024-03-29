export default function getInputHTMLType(controlType: string | undefined) {
    if (!controlType) {
        return 'text';
    }

    switch (controlType) {
        case 'DATE':
            return 'date';
        case 'DATE_TIME':
            return 'datetime-local';
        case 'EMAIL':
            return 'email';
        case 'NUMBER':
            return 'text';
        case 'PASSWORD':
            return 'password';
        case 'PHONE':
            return 'tel';
        case 'TIME':
            return 'time';
        case 'URL':
            return 'url';
        default:
            return 'text';
    }
}
