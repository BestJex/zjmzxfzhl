package com.zjmzxfzhl.common.validator;

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.internal.util.logging.Log;
import org.hibernate.validator.internal.util.logging.LoggerFactory;

import com.zjmzxfzhl.common.validator.constraints.LengthForGBK;

public class LengthForGBKValidator implements ConstraintValidator<LengthForGBK, String> {

	private static final Log LOG = LoggerFactory.make(MethodHandles.lookup());
	private static final String charsetName = "GBK";

	private int min;
	private int max;

	@Override
	public void initialize(LengthForGBK parameters) {
		min = parameters.min();
		max = parameters.max();
		validateParameters();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
		if (value == null) {
			return true;
		}
		int length = 0;
		try {
			length = value.getBytes(charsetName).length;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}
		return length >= min && length <= max;
	}

	private void validateParameters() {
		if (min < 0) {
			throw LOG.getMinCannotBeNegativeException();
		}
		if (max < 0) {
			throw LOG.getMaxCannotBeNegativeException();
		}
		if (max < min) {
			throw LOG.getLengthCannotBeNegativeException();
		}
	}
}
