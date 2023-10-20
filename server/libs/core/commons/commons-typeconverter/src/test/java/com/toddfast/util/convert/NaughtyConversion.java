package com.toddfast.util.convert;

/**
 * Potentially returns a different set of
 *
 * @author Todd Fast
 */
public class NaughtyConversion implements TypeConverter.Conversion {

	public Object[] getTypeKeys() {
		return keys;
	}

	// Don't do this for real!
	/*pkg*/ void setTypeKeys(Object[] value) {
		keys=value;
	}

	public Object convert(Object value) {
		return null;
	}

	private Object[] keys=new Object[] { Bogus.class };
}
