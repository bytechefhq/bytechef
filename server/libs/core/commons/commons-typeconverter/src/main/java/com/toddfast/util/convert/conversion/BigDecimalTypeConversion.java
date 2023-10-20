package com.toddfast.util.convert.conversion;

import com.toddfast.util.convert.TypeConverter;
import java.math.BigDecimal;

/**
 * Convert to a {@link BigDecimal} by parsing the value as a string
 *
 * @author Todd Fast
 */
public class BigDecimalTypeConversion implements TypeConverter.Conversion {

	@Override
	public Object[] getTypeKeys() {
		return new Object[] {
			BigDecimal.class,
			BigDecimal.class.getName(),
			TypeConverter.TYPE_BIG_DECIMAL
		};
	}

	public Object convert(Object value) {
		if (value==null) {
			return null;
		}
		if (!(value instanceof BigDecimal)) {
			String v=value.toString();
			if (v.trim().length()==0) {
				value=null;
			}
			else {
				value=new BigDecimal(v);
			}
		}
		return value;
	}
}
