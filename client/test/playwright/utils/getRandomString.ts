import {v4 as uuidv4} from 'uuid';

export default function getRandomString(): string {
    return uuidv4();
}
