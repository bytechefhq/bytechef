package com.toddfast.util.convert.conversion;

import com.toddfast.util.convert.TypeConverter;

/**
 * Convert to a character by parsing the first character of the value
 * as a string
 *
 * @author Todd Fast
 */
public class CharacterTypeConversion implements TypeConverter.Conversion {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] {
			Character.class,
			Character.TYPE,
			Character.class.getName(),
			TypeConverter.TYPE_CHAR,
			TypeConverter.TYPE_CHARACTER,
		};
	}

	@Override
	public Object convert(Object value) {
		if (value==null) {
			return null;
		}
		if (!(value instanceof Character)) {
			String v=value.toString();
			if (v.trim().length()==0) {
				value=null;
			}
			else {
				value=new Character(v.charAt(0));
			}
		}
		return value;
	}
}
