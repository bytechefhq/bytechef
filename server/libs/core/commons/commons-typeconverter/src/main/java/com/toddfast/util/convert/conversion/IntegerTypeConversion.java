package com.toddfast.util.convert.conversion;

import com.toddfast.util.convert.TypeConverter;

/**
 * Convert to an integer by parsing the value as a string
 *
 * @author Todd Fast
 */
public class IntegerTypeConversion implements TypeConverter.Conversion {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] {
			Integer.class,
			Integer.TYPE,
			Integer.class.getName(),
			TypeConverter.TYPE_INT,
			TypeConverter.TYPE_INTEGER
		};
	}

	@Override
	public Object convert(Object value) {
		if (value==null) {
			return null;
		}
		if (!(value instanceof Integer)) {
			String v=value.toString();
			if (v.trim().length()==0) {
				value=null;
			}
			else {
				value=Integer.parseInt(v);
			}
		}
		return value;
	}
}
