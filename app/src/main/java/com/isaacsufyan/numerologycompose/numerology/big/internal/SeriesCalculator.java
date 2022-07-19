package com.isaacsufyan.numerologycompose.numerology.big.internal;


import com.isaacsufyan.numerologycompose.numerology.big.BigRational;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public abstract class SeriesCalculator {

	private final boolean calculateInPairs;

	private final List<BigRational> factors = new ArrayList<>();

	protected SeriesCalculator() {
		this(false);
	}

	protected SeriesCalculator(boolean calculateInPairs) {
		this.calculateInPairs = calculateInPairs;
	}

	public BigDecimal calculate(BigDecimal x, MathContext mathContext) {
		BigDecimal acceptableError = BigDecimal.ONE.movePointLeft(mathContext.getPrecision() + 1);

		PowerIterator powerIterator = createPowerIterator(x, mathContext);

		BigDecimal sum = BigDecimal.ZERO;
		BigDecimal step;
		int i = 0;
		do {
			BigRational factor;
			BigDecimal xToThePower;

			factor = getFactor(i);
			xToThePower  = powerIterator.getCurrentPower();
			powerIterator.calculateNextPower();
			step = factor.getNumerator().multiply(xToThePower).divide(factor.getDenominator(), mathContext);
			i++;

			if (calculateInPairs) {
				factor = getFactor(i);
				xToThePower = powerIterator.getCurrentPower();
				powerIterator.calculateNextPower();
				BigDecimal step2 = factor.getNumerator().multiply(xToThePower).divide(factor.getDenominator(), mathContext);
				step = step.add(step2);
				i++;
			}

			sum = sum.add(step);
			//System.out.println(sum + " " + step);
		} while (step.abs().compareTo(acceptableError) > 0);

		return sum.round(mathContext);
	}

	protected abstract PowerIterator createPowerIterator(BigDecimal x, MathContext mathContext);

	protected synchronized BigRational getFactor(int index) {
		while (factors.size() <= index) {
			BigRational factor = getCurrentFactor();
			addFactor(factor);
			calculateNextFactor();
		}
		return factors.get(index);
	}

	private void addFactor(BigRational factor){
		factors.add(requireNonNull(factor, "Factor cannot be null"));
	}

	protected abstract BigRational getCurrentFactor();

	protected abstract void calculateNextFactor();
}
