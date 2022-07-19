package com.isaacsufyan.numerologycompose.numerology.big;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class BigRational extends Number implements Comparable<BigRational>, Serializable {

	public static final BigRational ZERO = new BigRational(0);

	public static final BigRational ONE = new BigRational(1);

	public static final BigRational TWO = new BigRational(2);

	public static final BigRational TEN = new BigRational(10);

	private final BigDecimal numerator;

	private final BigDecimal denominator;

	private BigRational(int value) {
		this(BigDecimal.valueOf(value), BigDecimal.ONE);
	}

	private BigRational(BigDecimal num, BigDecimal denom) {
		BigDecimal n = num;
		BigDecimal d = denom;

		if (d.signum() == 0) {
			throw new ArithmeticException("Divide by zero");
		}

		if (d.signum() < 0) {
			n = n.negate();
			d = d.negate();
		}

		numerator = n;
		denominator = d;
	}


	public BigInteger getNumeratorBigInteger() {
		return numerator.toBigInteger();
	}

	public BigDecimal getNumerator() {
		return numerator;
	}

	public BigInteger getDenominatorBigInteger() {
		return denominator.toBigInteger();
	}

	public BigDecimal getDenominator() {
		return denominator;
	}

	public BigRational reduce() {
		BigInteger n = numerator.toBigInteger();
		BigInteger d = denominator.toBigInteger();

		BigInteger gcd = n.gcd(d);
		n = n.divide(gcd);
		d = d.divide(gcd);

		return valueOf(n, d);
	}

	public BigRational integerPart() {
		return of(numerator.subtract(numerator.remainder(denominator)), denominator);
	}

	public BigRational fractionPart() {
		return of(numerator.remainder(denominator), denominator);
	}

	public BigRational negate() {
		if (isZero()) {
			return this;
		}

		return of(numerator.negate(), denominator);
	}

	public BigRational reciprocal() {
		return of(denominator, numerator);
	}

	public BigRational abs() {
		return isPositive() ? this : negate();
	}

	public int signum() {
		return numerator.signum();
	}

	public BigRational increment() {
		return of(numerator.add(denominator), denominator);
	}

	public BigRational decrement() {
		return of(numerator.subtract(denominator), denominator);
	}

	public BigRational add(BigRational value) {
		if (denominator.equals(value.denominator)) {
			return of(numerator.add(value.numerator), denominator);
		}

		BigDecimal n = numerator.multiply(value.denominator).add(value.numerator.multiply(denominator));
		BigDecimal d = denominator.multiply(value.denominator);
		return of(n, d);
	}

	private BigRational add(BigDecimal value) {
		return of(numerator.add(value.multiply(denominator)), denominator);
	}

	public BigRational add(BigInteger value) {
		if (value.equals(BigInteger.ZERO)) {
			return this;
		}
		return add(new BigDecimal(value));
	}

	public BigRational add(int value) {
		if (value == 0) {
			return this;
		}
		return add(BigInteger.valueOf(value));
	}

	public BigRational subtract(BigRational value) {
		if (denominator.equals(value.denominator)) {
			return of(numerator.subtract(value.numerator), denominator);
		}

		BigDecimal n = numerator.multiply(value.denominator).subtract(value.numerator.multiply(denominator));
		BigDecimal d = denominator.multiply(value.denominator);
		return of(n, d);
	}

	private BigRational subtract(BigDecimal value) {
		return of(numerator.subtract(value.multiply(denominator)), denominator);
	}

	public BigRational subtract(BigInteger value) {
		if (value.equals(BigInteger.ZERO)) {
			return this;
		}
		return subtract(new BigDecimal(value));
	}

	public BigRational subtract(int value) {
		if (value == 0) {
			return this;
		}
		return subtract(BigInteger.valueOf(value));
	}


	public BigRational multiply(BigRational value) {
		if (isZero() || value.isZero()) {
			return ZERO;
		}
		if (equals(ONE)) {
			return value;
		}
		if (value.equals(ONE)) {
			return this;
		}

		BigDecimal n = numerator.multiply(value.numerator);
		BigDecimal d = denominator.multiply(value.denominator);
		return of(n, d);
	}

	// private, because we want to hide that we use BigDecimal internally
	private BigRational multiply(BigDecimal value) {
		BigDecimal n = numerator.multiply(value);
		BigDecimal d = denominator;
		return of(n, d);
	}


	public BigRational multiply(BigInteger value) {
		if (isZero() || value.signum() == 0) {
			return ZERO;
		}
		if (equals(ONE)) {
			return valueOf(value);
		}
		if (value.equals(BigInteger.ONE)) {
			return this;
		}

		return multiply(new BigDecimal(value));
	}

	public BigRational multiply(int value) {
		return multiply(BigInteger.valueOf(value));
	}

	public BigRational divide(BigRational value) {
		if (value.equals(ONE)) {
			return this;
		}

		BigDecimal n = numerator.multiply(value.denominator);
		BigDecimal d = denominator.multiply(value.numerator);
		return of(n, d);
	}

	private BigRational divide(BigDecimal value) {
		BigDecimal n = numerator;
		BigDecimal d = denominator.multiply(value);
		return of(n, d);
	}

	public BigRational divide(BigInteger value) {
		if (value.equals(BigInteger.ONE)) {
			return this;
		}

		return divide(new BigDecimal(value));
	}

	public BigRational divide(int value) {
		return divide(BigInteger.valueOf(value));
	}

	public boolean isZero() {
		return numerator.signum() == 0;
	}

	private boolean isPositive() {
		return numerator.signum() > 0;
	}

	public boolean isInteger() {
		return isIntegerInternal() || reduce().isIntegerInternal();
	}

	private boolean isIntegerInternal() {
		return denominator.compareTo(BigDecimal.ONE) == 0;
	}

	public BigRational pow(int exponent) {
		if (exponent == 0) {
			return ONE;
		}
		if (exponent == 1) {
			return this;
		}

		final BigInteger n;
		final BigInteger d;
		if (exponent > 0) {
			n = numerator.toBigInteger().pow(exponent);
			d = denominator.toBigInteger().pow(exponent);
		}
		else {
			n = denominator.toBigInteger().pow(-exponent);
			d = numerator.toBigInteger().pow(-exponent);
		}
		return valueOf(n, d);
	}

	private BigRational min(BigRational value) {
		return compareTo(value) <= 0 ? this : value;
	}

	private BigRational max(BigRational value) {
		return compareTo(value) >= 0 ? this : value;
	}

	public BigRational withPrecision(int precision) {
		return valueOf(toBigDecimal(new MathContext(precision)));
	}

	public BigRational withScale(int scale) {
		return valueOf(toBigDecimal().setScale(scale, RoundingMode.HALF_UP));
	}

	private static int countDigits(BigInteger number) {
		double factor = Math.log(2) / Math.log(10);
		int digitCount = (int) (factor * number.bitLength() + 1);
		if (BigInteger.TEN.pow(digitCount - 1).compareTo(number) > 0) {
			return digitCount - 1;
		}
		return digitCount;
	}

	// TODO what is precision of a rational?
	private int precision() {
		return countDigits(numerator.toBigInteger()) + countDigits(denominator.toBigInteger());
	}

	public double toDouble() {
		return toBigDecimal().doubleValue();
	}

	public float toFloat() {
		return toBigDecimal().floatValue();
	}

	public BigDecimal toBigDecimal() {
		int precision = Math.max(precision(), MathContext.DECIMAL128.getPrecision());
		return toBigDecimal(new MathContext(precision));
	}

	public BigDecimal toBigDecimal(MathContext mc) {
		return numerator.divide(denominator, mc);
	}

	@Override
	public int compareTo(BigRational other) {
		if (this == other) {
			return 0;
		}
		return numerator.multiply(other.denominator).compareTo(denominator.multiply(other.numerator));
	}

	@Override
	public int hashCode() {
		if (isZero()) {
			return 0;
		}
		return numerator.hashCode() + denominator.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof BigRational)) {
			return false;
		}

		BigRational other = (BigRational) obj;
		if (!numerator.equals(other.numerator)) {
			return false;
		}
		return denominator.equals(other.denominator);
	}

	@Override
	public String toString() {
		if (isZero()) {
			return "0";
		}
		if (isIntegerInternal()) {
			return numerator.toString();
		}
		return toBigDecimal().toString();
	}

	public String toPlainString() {
		if (isZero()) {
			return "0";
		}
		if (isIntegerInternal()) {
			return numerator.toPlainString();
		}
		return toBigDecimal().toPlainString();
	}

	public String toRationalString() {
		if (isZero()) {
			return "0";
		}
		if (isIntegerInternal()) {
			return numerator.toString();
		}
		return numerator + "/" + denominator;
	}

	public String toIntegerRationalString() {
		BigDecimal fractionNumerator = numerator.remainder(denominator);
		BigDecimal integerNumerator = numerator.subtract(fractionNumerator);
		BigDecimal integerPart = integerNumerator.divide(denominator);

		StringBuilder result = new StringBuilder();
		if (integerPart.signum() != 0) {
			result.append(integerPart);
		}
		if (fractionNumerator.signum() != 0) {
			if (result.length() > 0) {
				result.append(' ');
				result.append(fractionNumerator.abs());
			} else {
				result.append(fractionNumerator);
			}
			result.append('/');
			result.append(denominator);
		}
		if (result.length() == 0) {
			result.append('0');
		}

		return result.toString();
	}

	public static BigRational valueOf(int value) {
		if (value == 0) {
			return ZERO;
		}
		if (value == 1) {
			return ONE;
		}
		return new BigRational(value);
	}

	public static BigRational valueOf(int numerator, int denominator) {
		return of(BigDecimal.valueOf(numerator), BigDecimal.valueOf(denominator));
	}

	public static BigRational valueOf(int integer, int fractionNumerator, int fractionDenominator) {
		if (fractionNumerator < 0 || fractionDenominator < 0) {
			throw new ArithmeticException("Negative value");
		}

		BigRational integerPart = valueOf(integer);
		BigRational fractionPart = valueOf(fractionNumerator, fractionDenominator);
		return integerPart.isPositive() ? integerPart.add(fractionPart) : integerPart.subtract(fractionPart);
	}

	public static BigRational valueOf(BigInteger numerator, BigInteger denominator) {
		return of(new BigDecimal(numerator), new BigDecimal(denominator));
	}

	public static BigRational valueOf(BigInteger value) {
		if (value.compareTo(BigInteger.ZERO) == 0) {
			return ZERO;
		}
		if (value.compareTo(BigInteger.ONE) == 0) {
			return ONE;
		}
		return valueOf(value, BigInteger.ONE);
	}

	public static BigRational valueOf(double value) {
		if (value == 0.0) {
			return ZERO;
		}
		if (value == 1.0) {
			return ONE;
		}
		if (Double.isInfinite(value)) {
			throw new NumberFormatException("Infinite");
		}
		if (Double.isNaN(value)) {
			throw new NumberFormatException("NaN");
		}
		return valueOf(new BigDecimal(String.valueOf(value)));
	}

	public static BigRational valueOf(BigDecimal value) {
		if (value.compareTo(BigDecimal.ZERO) == 0) {
			return ZERO;
		}
		if (value.compareTo(BigDecimal.ONE) == 0) {
			return ONE;
		}

		int scale = value.scale();
		if (scale == 0) {
			return new BigRational(value, BigDecimal.ONE);
		} else if (scale < 0) {
			BigDecimal n = new BigDecimal(value.unscaledValue()).multiply(BigDecimal.ONE.movePointLeft(value.scale()));
			return new BigRational(n, BigDecimal.ONE);
		}
		else {
			BigDecimal n = new BigDecimal(value.unscaledValue());
			BigDecimal d = BigDecimal.ONE.movePointRight(value.scale());
			return new BigRational(n, d);
		}
	}

	public static BigRational valueOf(String string) {
		String[] strings = string.split("/");
		BigRational result = valueOfSimple(strings[0]);
		for (int i = 1; i < strings.length; i++) {
			result = result.divide(valueOfSimple(strings[i]));
		}
		return result;
	}

	private static BigRational valueOfSimple(String string) {
		return valueOf(new BigDecimal(string));
	}

	public static BigRational valueOf(boolean positive, String integerPart, String fractionPart, String fractionRepeatPart, String exponentPart) {
		BigRational result = ZERO;

		if (fractionRepeatPart != null && fractionRepeatPart.length() > 0) {
			BigInteger lotsOfNines = BigInteger.TEN.pow(fractionRepeatPart.length()).subtract(BigInteger.ONE);
			result = valueOf(new BigInteger(fractionRepeatPart), lotsOfNines);
		}

		if (fractionPart != null && fractionPart.length() > 0) {
			result = result.add(valueOf(new BigInteger(fractionPart)));
			result = result.divide(BigInteger.TEN.pow(fractionPart.length()));
		}

		if (integerPart != null && integerPart.length() > 0) {
			result = result.add(new BigInteger(integerPart));
		}

		if (exponentPart != null && exponentPart.length() > 0) {
			int exponent = Integer.parseInt(exponentPart);
			BigInteger powerOfTen = BigInteger.TEN.pow(Math.abs(exponent));
			result = exponent >= 0 ? result.multiply(powerOfTen) : result.divide(powerOfTen);
		}

		if (!positive) {
			result = result.negate();
		}

		return result;
	}

	public static BigRational valueOf(BigDecimal numerator, BigDecimal denominator) {
		return valueOf(numerator).divide(valueOf(denominator));
	}

	private static BigRational of(BigDecimal numerator, BigDecimal denominator) {
		if (numerator.signum() == 0 && denominator.signum() != 0) {
			return ZERO;
		}
		if (numerator.compareTo(BigDecimal.ONE) == 0 && denominator.compareTo(BigDecimal.ONE) == 0) {
			return ONE;
		}
		return new BigRational(numerator, denominator);
	}

	public static BigRational min(BigRational... values) {
		if (values.length == 0) {
			return BigRational.ZERO;
		}
		BigRational result = values[0];
		for (int i = 1; i < values.length; i++) {
			result = result.min(values[i]);
		}
		return result;
	}

	public static BigRational max(BigRational... values) {
		if (values.length == 0) {
			return BigRational.ZERO;
		}
		BigRational result = values[0];
		for (int i = 1; i < values.length; i++) {
			result = result.max(values[i]);
		}
		return result;
	}

	private static List<BigRational> bernoulliCache = new ArrayList<>();

    public static BigRational bernoulli(int n) {
		if (n < 0) {
			throw new ArithmeticException("Illegal bernoulli(n) for n < 0: n = " + n);
		}
    	if (n == 1) {
    		return valueOf(-1, 2);
    	} else if (n % 2 == 1) {
    		return ZERO;
    	}

    	synchronized (bernoulliCache) {
    		int index = n / 2;

    		if (bernoulliCache.size() <= index) {
    			for (int i = bernoulliCache.size(); i <= index; i++) {
    				BigRational b = calculateBernoulli(i * 2);
					bernoulliCache.add(b);
				}
    		}

    		return bernoulliCache.get(index);
		}
    }

    private static BigRational calculateBernoulli(int n) {
    	return IntStream.rangeClosed(0, n).parallel().mapToObj(k -> {
            BigRational jSum = ZERO ;
            BigRational bin = ONE ;
            for(int j=0 ; j <= k ; j++) {
                BigRational jPowN = valueOf(j).pow(n);
                if (j % 2 == 0) {
                	jSum = jSum.add(bin.multiply(jPowN)) ;
                } else {
                	jSum = jSum.subtract(bin.multiply(jPowN)) ;
                }

                bin = bin.multiply(valueOf(k-j).divide(valueOf(j+1)));
            }
            return jSum.divide(valueOf(k+1));
    	}).reduce(ZERO, BigRational::add);
    }

	@Override
	public int intValue() {
		return toBigDecimal().intValue();
	}

	@Override
	public long longValue() {
		return toBigDecimal().longValue();
	}

	@Override
	public float floatValue() {
		return toFloat();
	}

	@Override
	public double doubleValue() {
		return toDouble();
	}
}
