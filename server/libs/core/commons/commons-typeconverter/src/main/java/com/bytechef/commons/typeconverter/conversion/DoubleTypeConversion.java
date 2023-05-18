package com.bytechef.commons.typeconverter.conversion;

import com.bytechef.commons.typeconverter.TypeConverter;

/**
 * Convert to a double by parsing the value as a string
 *
 * @author Todd Fast
 */
public class DoubleTypeConversion implements TypeConverter.Conversion {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] {
			Double.class,
			Double.TYPE,
			Double.class.getName(),
			TypeConverter.TYPE_DOUBLE
		};
	}

	@Override
	public Object convert(Object value) {
		if (value==null) {
			return null;
		}
		if (!(value instanceof Double)) {
			String v=value.toString();
			if (v.trim().length()==0) {
				value=null;
			}
			else {
				value=Double.parseDouble(v);
			}
		}
		return value;
	}
}
