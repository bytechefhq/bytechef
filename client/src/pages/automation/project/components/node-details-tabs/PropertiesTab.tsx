import Properties from 'components/Properties/Properties';
import {PropertyType} from 'types/projectTypes';

const PropertiesTab = ({properties}: {properties: Array<PropertyType>}) => (
    <Properties properties={properties} />
);

export default PropertiesTab;
