package com.isaacsufyan.numerologycompose.numerology.big.internal;

import java.math.BigDecimal;

public interface PowerIterator {
	BigDecimal getCurrentPower();
	void calculateNextPower();
}
