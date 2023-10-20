package com.bytechef.commons.typeconverter.conversion;

import com.bytechef.commons.typeconverter.TypeConverter;

/**
 * Convert to a byte by parsing the value as a string
 *
 * @author Todd Fast
 */
public class ByteTypeConversion implements TypeConverter.Conversion {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] {
			Byte.class,
			Byte.TYPE,
			Byte.class.getName(),
			TypeConverter.TYPE_BYTE
		};
	}

	@Override
	public Object convert(Object value) {
		if (value==null) {
			return null;
		}
		if (!(value instanceof Byte)) {
			String v=value.toString();
			if (v.trim().length()==0) {
				value=null;
			}
			else {
				value=Byte.parseByte(v);
			}
		}
		return value;
	}
}
