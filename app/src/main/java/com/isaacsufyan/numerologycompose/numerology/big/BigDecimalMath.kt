package com.isaacsufyan.numerologycompose.numerology.big

import kotlin.jvm.Volatile
import com.isaacsufyan.numerologycompose.numerology.big.BigDecimalMath
import kotlin.jvm.JvmOverloads
import com.isaacsufyan.numerologycompose.numerology.big.internal.ExpCalculator
import java.lang.ArithmeticException
import java.lang.NumberFormatException
import java.lang.UnsupportedOperationException
import java.math.BigDecimal
import java.math.MathContext
import java.util.*

object BigDecimalMath {

    private val TWO = BigDecimal.valueOf(2)
    private val THREE = BigDecimal.valueOf(3)
    private val MINUS_ONE = BigDecimal.valueOf(-1)
    private val ONE_HALF = BigDecimal.valueOf(0.5)
    private val DOUBLE_MAX_VALUE = BigDecimal.valueOf(Double.MAX_VALUE)

    @Volatile
    private var log2Cache: BigDecimal? = null
    private val log2CacheLock = Any()

    @Volatile
    private var log3Cache: BigDecimal? = null
    private val log3CacheLock = Any()

    @Volatile
    private var log10Cache: BigDecimal? = null
    private val log10CacheLock = Any()

    @Volatile
    private var piCache: BigDecimal? = null
    private val piCacheLock = Any()

    @Volatile
    private var eCache: BigDecimal? = null
    private val eCacheLock = Any()
    private val ROUGHLY_TWO_PI = BigDecimal("3.141592653589793").multiply(TWO)
    private const val EXPECTED_INITIAL_PRECISION = 15
    private val factorialCache = arrayOfNulls<BigDecimal>(100)
    private val spougeFactorialConstantsCache: MutableMap<Int, List<BigDecimal>> = HashMap()
    private val spougeFactorialConstantsCacheLock = Any()

    @JvmOverloads
    fun toBigDecimal(string: String, mathContext: MathContext = MathContext.UNLIMITED): BigDecimal {
        val len = string.length
        if (len < 600) {
            return BigDecimal(string, mathContext)
        }
        val splitLength = len / if (len >= 10000) 8 else 5
        return toBigDecimal(string, mathContext, splitLength)
    }

    fun toBigDecimal(string: String, mathContext: MathContext, splitLength: Int): BigDecimal {
        val len = string.length
        if (len < splitLength) {
            return BigDecimal(string, mathContext)
        }
        val chars = string.toCharArray()
        var numberHasSign = false
        var negative = false
        var numberIndex = 0
        var dotIndex = -1
        var expIndex = -1
        var expHasSign = false
        var scale = 0
        for (i in 0 until len) {
            val c = chars[i]
            when (c) {
                '+' -> if (expIndex >= 0) {
                    if (expHasSign) {
                        throw NumberFormatException("Multiple signs in exponent")
                    }
                    expHasSign = true
                } else {
                    if (numberHasSign) {
                        throw NumberFormatException("Multiple signs in number")
                    }
                    numberHasSign = true
                    numberIndex = i + 1
                }
                '-' -> if (expIndex >= 0) {
                    if (expHasSign) {
                        throw NumberFormatException("Multiple signs in exponent")
                    }
                    expHasSign = true
                } else {
                    if (numberHasSign) {
                        throw NumberFormatException("Multiple signs in number")
                    }
                    numberHasSign = true
                    negative = true
                    numberIndex = i + 1
                }
                'e', 'E' -> {
                    if (expIndex >= 0) {
                        throw NumberFormatException("Multiple exponent markers")
                    }
                    expIndex = i
                }
                '.' -> {
                    if (dotIndex >= 0) {
                        throw NumberFormatException("Multiple decimal points")
                    }
                    dotIndex = i
                }
                else -> if (dotIndex >= 0 && expIndex == -1) {
                    scale++
                }
            }
        }
        val numberEndIndex: Int
        var exp = 0
        if (expIndex >= 0) {
            numberEndIndex = expIndex
            val expString = String(chars, expIndex + 1, len - expIndex - 1)
            exp = expString.toInt()
            scale = adjustScale(scale, exp.toLong())
        } else {
            numberEndIndex = len
        }
        var result: BigDecimal
        result = if (dotIndex >= 0) {
            val leftLength = dotIndex - numberIndex
            val bigDecimalLeft =
                toBigDecimalRecursive(chars, numberIndex, leftLength, exp, splitLength)
            val rightLength = numberEndIndex - dotIndex - 1
            val bigDecimalRight = toBigDecimalRecursive(
                chars,
                dotIndex + 1,
                rightLength,
                exp - rightLength,
                splitLength
            )
            bigDecimalLeft.add(bigDecimalRight)
        } else {
            toBigDecimalRecursive(
                chars,
                numberIndex,
                numberEndIndex - numberIndex,
                exp,
                splitLength
            )
        }
        if (scale != 0) {
            result = result.setScale(scale)
        }
        if (negative) {
            result = result.negate()
        }
        if (mathContext.precision != 0) {
            result = result.round(mathContext)
        }
        return result
    }

    private fun adjustScale(scale: Int, exp: Long): Int {
        val adjustedScale = scale - exp
        if (adjustedScale > Int.MAX_VALUE || adjustedScale < Int.MIN_VALUE) throw NumberFormatException(
            "Scale out of range: $adjustedScale while adjusting scale $scale to exponent $exp"
        )
        return adjustedScale.toInt()
    }

    private fun toBigDecimalRecursive(
        chars: CharArray,
        offset: Int,
        length: Int,
        scale: Int,
        splitLength: Int
    ): BigDecimal {
        if (length > splitLength) {
            val mid = length / 2
            val bigDecimalLeft =
                toBigDecimalRecursive(chars, offset, mid, scale + length - mid, splitLength)
            val bigDecimalRight =
                toBigDecimalRecursive(chars, offset + mid, length - mid, scale, splitLength)
            return bigDecimalLeft.add(bigDecimalRight)
        }
        return if (length == 0) {
            BigDecimal.ZERO
        } else BigDecimal(chars, offset, length).movePointRight(scale)
    }

    fun isIntValue(value: BigDecimal): Boolean {
        // TODO impl isIntValue() without exceptions
        try {
            value.intValueExact()
            return true
        } catch (ex: ArithmeticException) {
            // ignored
        }
        return false
    }

    fun isLongValue(value: BigDecimal): Boolean {
        // TODO impl isLongValue() without exceptions
        try {
            value.longValueExact()
            return true
        } catch (ex: ArithmeticException) {
            // ignored
        }
        return false
    }

    fun isDoubleValue(value: BigDecimal): Boolean {
        if (value.compareTo(DOUBLE_MAX_VALUE) > 0) {
            return false
        }
        return if (value.compareTo(DOUBLE_MAX_VALUE.negate()) < 0) {
            false
        } else true
    }

    fun mantissa(value: BigDecimal): BigDecimal {
        val exponent = exponent(value)
        return if (exponent == 0) {
            value
        } else value.movePointLeft(exponent)
    }

    fun exponent(value: BigDecimal): Int {
        return value.precision() - value.scale() - 1
    }

    fun significantDigits(value: BigDecimal): Int {
        val stripped = value.stripTrailingZeros()
        return if (stripped.scale() >= 0) {
            stripped.precision()
        } else {
            stripped.precision() - stripped.scale()
        }
    }

    fun integralPart(value: BigDecimal): BigDecimal {
        return value.setScale(0, BigDecimal.ROUND_DOWN)
    }

    fun fractionalPart(value: BigDecimal): BigDecimal {
        return value.subtract(integralPart(value))
    }

    fun round(value: BigDecimal?, mathContext: MathContext?): BigDecimal {
        return value!!.round(mathContext)
    }

    fun roundWithTrailingZeroes(value: BigDecimal, mathContext: MathContext): BigDecimal {
        if (value.precision() == mathContext.precision) {
            return value
        }
        return if (value.signum() == 0) {
            BigDecimal.ZERO.setScale(mathContext.precision - 1)
        } else try {
            val stripped = value.stripTrailingZeros()
            val exponentStripped = exponent(stripped) // value.precision() - value.scale() - 1;
            val zero: BigDecimal
            zero = if (exponentStripped < -1) {
                BigDecimal.ZERO.setScale(mathContext.precision - exponentStripped)
            } else {
                BigDecimal.ZERO.setScale(mathContext.precision + exponentStripped + 1)
            }
            stripped.add(zero, mathContext)
        } catch (ex: ArithmeticException) {
            value.round(mathContext)
        }
    }

    fun reciprocal(x: BigDecimal?, mathContext: MathContext?): BigDecimal {
        return BigDecimal.ONE.divide(x, mathContext)
    }

    fun factorial(n: Int): BigDecimal? {
        if (n < 0) {
            throw ArithmeticException("Illegal factorial(n) for n < 0: n = $n")
        }
        if (n < factorialCache.size) {
            return factorialCache[n]
        }
        val result = factorialCache[factorialCache.size - 1]
        return result!!.multiply(factorialRecursion(factorialCache.size, n))
    }

    private fun factorialLoop(n1: Int, n2: Int): BigDecimal {
        var n1 = n1
        val limit = Long.MAX_VALUE / n2
        var accu: Long = 1
        var result = BigDecimal.ONE
        while (n1 <= n2) {
            if (accu <= limit) {
                accu *= n1.toLong()
            } else {
                result = result.multiply(BigDecimal.valueOf(accu))
                accu = n1.toLong()
            }
            n1++
        }
        return result.multiply(BigDecimal.valueOf(accu))
    }

    private fun factorialRecursion(n1: Int, n2: Int): BigDecimal {
        val threshold = if (n1 > 200) 80 else 150
        if (n2 - n1 < threshold) {
            return factorialLoop(n1, n2)
        }
        val mid = n1 + n2 shr 1
        return factorialRecursion(mid + 1, n2).multiply(factorialRecursion(n1, mid))
    }

    fun factorial(x: BigDecimal, mathContext: MathContext): BigDecimal {
        if (isIntValue(x)) {
            return round(factorial(x.intValueExact()), mathContext)
        }
        checkMathContext(mathContext)
        val mc = MathContext(mathContext.precision shl 1, mathContext.roundingMode)
        val a = mathContext.precision * 13 / 10
        val constants = getSpougeFactorialConstants(a)
        val bigA = BigDecimal.valueOf(a.toLong())
        var negative = false
        var factor = constants[0]
        for (k in 1 until a) {
            val bigK = BigDecimal.valueOf(k.toLong())
            factor = factor.add(constants[k].divide(x.add(bigK), mc))
            negative = !negative
        }
        var result = pow(x.add(bigA), x.add(BigDecimal.valueOf(0.5)), mc)
        result = result.multiply(exp(x.negate().subtract(bigA), mc))
        result = result.multiply(factor)
        return round(result, mathContext)
    }

    fun getSpougeFactorialConstants(a: Int): List<BigDecimal> {
        synchronized(spougeFactorialConstantsCacheLock) {
            return spougeFactorialConstantsCache.computeIfAbsent(a) { key: Int? ->
                val constants: MutableList<BigDecimal> = ArrayList(a)
                val mc = MathContext(a * 15 / 10)
                val c0 = sqrt(
                    pi(mc)!!.multiply(TWO, mc), mc
                )
                constants.add(c0)
                var negative = false
                for (k in 1 until a) {
                    val bigK = BigDecimal.valueOf(k.toLong())
                    val deltaAK = a.toLong() - k
                    var ck = pow(BigDecimal.valueOf(deltaAK), bigK.subtract(ONE_HALF), mc)
                    ck = ck.multiply(exp(BigDecimal.valueOf(deltaAK), mc), mc)
                    ck = ck.divide(factorial(k - 1), mc)
                    if (negative) {
                        ck = ck.negate()
                    }
                    constants.add(ck)
                    negative = !negative
                }
                Collections.unmodifiableList(constants)
            }
        }
    }

    fun gamma(x: BigDecimal, mathContext: MathContext): BigDecimal {
        return factorial(x.subtract(BigDecimal.ONE), mathContext)
    }

    fun pow(x: BigDecimal, y: BigDecimal, mathContext: MathContext): BigDecimal {
        checkMathContext(mathContext)
        if (x.signum() == 0) {
            when (y.signum()) {
                0 -> return round(BigDecimal.ONE, mathContext)
                1 -> return round(BigDecimal.ZERO, mathContext)
            }
        }
        try {
            val longValue = y.longValueExact()
            return pow(x, longValue, mathContext)
        } catch (ex: ArithmeticException) {
            // ignored
        }
        if (fractionalPart(y).signum() == 0) {
            return powInteger(x, y, mathContext)
        }

        // x^y = exp(y*log(x))
        val mc = MathContext(mathContext.precision + 6, mathContext.roundingMode)
        val result = exp(y.multiply(log(x, mc), mc), mc)
        return round(result, mathContext)
    }

    fun pow(x: BigDecimal, y: Long, mathContext: MathContext): BigDecimal {
        var x = x
        var y = y
        val mc = if (mathContext.precision == 0) mathContext else MathContext(
            mathContext.precision + 10,
            mathContext.roundingMode
        )
        if (y < 0) {
            val value = reciprocal(pow(x, -y, mc), mc)
            return round(value, mathContext)
        }
        var result = BigDecimal.ONE
        while (y > 0) {
            if (y and 1 == 1L) {
                // odd exponent -> multiply result with x
                result = result.multiply(x, mc)
                y -= 1
            }
            if (y > 0) {
                // even exponent -> square x
                x = x.multiply(x, mc)
            }
            y = y shr 1
        }
        return round(result, mathContext)
    }

    private fun powInteger(
        x: BigDecimal,
        integerY: BigDecimal,
        mathContext: MathContext
    ): BigDecimal {
        var x = x
        var integerY = integerY
        require(fractionalPart(integerY).signum() == 0) { "Not integer value: $integerY" }
        if (integerY.signum() < 0) {
            return BigDecimal.ONE.divide(powInteger(x, integerY.negate(), mathContext), mathContext)
        }
        val mc = MathContext(
            Math.max(mathContext.precision, -integerY.scale()) + 30,
            mathContext.roundingMode
        )
        var result = BigDecimal.ONE
        while (integerY.signum() > 0) {
            var halfY = integerY.divide(TWO, mc)
            if (fractionalPart(halfY).signum() != 0) {
                // odd exponent -> multiply result with x
                result = result.multiply(x, mc)
                integerY = integerY.subtract(BigDecimal.ONE)
                halfY = integerY.divide(TWO, mc)
            }
            if (halfY.signum() > 0) {
                // even exponent -> square x
                x = x.multiply(x, mc)
            }
            integerY = halfY
        }
        return round(result, mathContext)
    }

    fun sqrt(x: BigDecimal, mathContext: MathContext): BigDecimal {
        checkMathContext(mathContext)
        when (x.signum()) {
            0 -> return BigDecimal.ZERO
            -1 -> throw ArithmeticException("Illegal sqrt(x) for x < 0: x = $x")
        }
        val maxPrecision = mathContext.precision + 6
        val acceptableError = BigDecimal.ONE.movePointLeft(mathContext.precision + 1)
        var result: BigDecimal
        var adaptivePrecision: Int
        if (isDoubleValue(x)) {
            result = BigDecimal.valueOf(Math.sqrt(x.toDouble()))
            adaptivePrecision = EXPECTED_INITIAL_PRECISION
        } else {
            result = x.multiply(ONE_HALF, mathContext)
            adaptivePrecision = 1
        }
        var last: BigDecimal
        if (adaptivePrecision < maxPrecision) {
            if (result.multiply(result).compareTo(x) == 0) {
                return round(result, mathContext) // early exit if x is a square number
            }
            do {
                last = result
                adaptivePrecision = adaptivePrecision shl 1
                if (adaptivePrecision > maxPrecision) {
                    adaptivePrecision = maxPrecision
                }
                val mc = MathContext(adaptivePrecision, mathContext.roundingMode)
                result = x.divide(result, mc).add(last).multiply(ONE_HALF, mc)
            } while (adaptivePrecision < maxPrecision || result.subtract(last).abs()
                    .compareTo(acceptableError) > 0
            )
        }
        return round(result, mathContext)
    }

    fun root(x: BigDecimal, n: BigDecimal, mathContext: MathContext): BigDecimal {
        checkMathContext(mathContext)
        when (n.signum()) {
            -1, 0 -> throw ArithmeticException("Illegal root(x, n) for n <= 0: n = $n")
        }
        when (x.signum()) {
            0 -> return BigDecimal.ZERO
            -1 -> throw ArithmeticException("Illegal root(x, n) for x < 0: x = $x")
        }
        if (isDoubleValue(x) && isDoubleValue(n)) {
            val initialResult = Math.pow(x.toDouble(), 1.0 / n.toDouble())
            if (java.lang.Double.isFinite(initialResult)) {
                return rootUsingNewtonRaphson(x, n, BigDecimal.valueOf(initialResult), mathContext)
            }
        }
        val mc = MathContext(mathContext.precision + 6, mathContext.roundingMode)
        return pow(x, BigDecimal.ONE.divide(n, mc), mathContext)
    }

    private fun rootUsingNewtonRaphson(
        x: BigDecimal,
        n: BigDecimal,
        initialResult: BigDecimal,
        mathContext: MathContext
    ): BigDecimal {
        if (n.compareTo(BigDecimal.ONE) <= 0) {
            val mc = MathContext(mathContext.precision + 6, mathContext.roundingMode)
            return pow(x, BigDecimal.ONE.divide(n, mc), mathContext)
        }
        val maxPrecision = mathContext.precision * 2
        val acceptableError = BigDecimal.ONE.movePointLeft(mathContext.precision + 1)
        val nMinus1 = n.subtract(BigDecimal.ONE)
        var result = initialResult
        var adaptivePrecision = 12
        if (adaptivePrecision < maxPrecision) {
            var step: BigDecimal
            do {
                adaptivePrecision *= 3
                if (adaptivePrecision > maxPrecision) {
                    adaptivePrecision = maxPrecision
                }
                val mc = MathContext(adaptivePrecision, mathContext.roundingMode)
                step = x.divide(pow(result, nMinus1, mc), mc).subtract(result).divide(n, mc)
                result = result.add(step)
            } while (adaptivePrecision < maxPrecision || step.abs().compareTo(acceptableError) > 0)
        }
        return round(result, mathContext)
    }

    fun log(x: BigDecimal, mathContext: MathContext): BigDecimal {
        checkMathContext(mathContext)
        if (x.signum() <= 0) {
            throw ArithmeticException("Illegal log(x) for x <= 0: x = $x")
        }
        if (x.compareTo(BigDecimal.ONE) == 0) {
            return BigDecimal.ZERO
        }
        val result: BigDecimal?
        result = when (x.compareTo(BigDecimal.TEN)) {
            0 -> logTen(mathContext)
            1 -> logUsingExponent(x, mathContext)
            else -> logUsingTwoThree(x, mathContext)
        }
        return round(result, mathContext)
    }

    fun log2(x: BigDecimal, mathContext: MathContext): BigDecimal {
        checkMathContext(mathContext)
        val mc = MathContext(mathContext.precision + 4, mathContext.roundingMode)
        val result = log(x, mc).divide(logTwo(mc), mc)
        return round(result, mathContext)
    }

    fun log10(x: BigDecimal, mathContext: MathContext): BigDecimal {
        checkMathContext(mathContext)
        val mc = MathContext(mathContext.precision + 2, mathContext.roundingMode)
        val result = log(x, mc).divide(logTen(mc), mc)
        return round(result, mathContext)
    }

    private fun logUsingNewton(x: BigDecimal, mathContext: MathContext): BigDecimal {
        val maxPrecision = mathContext.precision + 20
        val acceptableError = BigDecimal.ONE.movePointLeft(mathContext.precision + 1)
        var result: BigDecimal
        var adaptivePrecision: Int
        val doubleX = x.toDouble()
        if (doubleX > 0.0 && isDoubleValue(x)) {
            result = BigDecimal.valueOf(Math.log(doubleX))
            adaptivePrecision = EXPECTED_INITIAL_PRECISION
        } else {
            result = x.divide(TWO, mathContext)
            adaptivePrecision = 1
        }
        var step: BigDecimal
        do {
            adaptivePrecision *= 3
            if (adaptivePrecision > maxPrecision) {
                adaptivePrecision = maxPrecision
            }
            val mc = MathContext(adaptivePrecision, mathContext.roundingMode)
            val expY = exp(result, mc)
            step = TWO.multiply(x.subtract(expY)).divide(x.add(expY), mc)
            //System.out.println("  step " + step + " adaptivePrecision=" + adaptivePrecision);
            result = result.add(step)
        } while (adaptivePrecision < maxPrecision || step.abs().compareTo(acceptableError) > 0)
        return result
    }

    private fun logUsingExponent(x: BigDecimal, mathContext: MathContext): BigDecimal {
        val mcDouble = MathContext(mathContext.precision shl 1, mathContext.roundingMode)
        val mc = MathContext(mathContext.precision + 4, mathContext.roundingMode)
        val exponent = exponent(x)
        val mantissa = mantissa(x)
        var result = logUsingTwoThree(mantissa, mc)
        if (exponent != 0) {
            result =
                result.add(BigDecimal.valueOf(exponent.toLong()).multiply(logTen(mcDouble), mc))
        }
        return result
    }

    private fun logUsingTwoThree(x: BigDecimal, mathContext: MathContext): BigDecimal {
        val mcDouble = MathContext(mathContext.precision shl 1, mathContext.roundingMode)
        val mc = MathContext(mathContext.precision + 4, mathContext.roundingMode)
        var factorOfTwo = 0
        var powerOfTwo = 1
        var factorOfThree = 0
        var powerOfThree = 1
        var value = x.toDouble()
        if (value < 0.01) {
            // do nothing
        } else if (value < 0.1) { // never happens when called by logUsingExponent()
            while (value < 0.6) {
                value *= 2.0
                factorOfTwo--
                powerOfTwo = powerOfTwo shl 1
            }
        } else if (value < 0.115) { // (0.1 - 0.11111 - 0.115) -> (0.9 - 1.0 - 1.035)
            factorOfThree = -2
            powerOfThree = 9
        } else if (value < 0.14) { // (0.115 - 0.125 - 0.14) -> (0.92 - 1.0 - 1.12)
            factorOfTwo = -3
            powerOfTwo = 8
        } else if (value < 0.2) { // (0.14 - 0.16667 - 0.2) - (0.84 - 1.0 - 1.2)
            factorOfTwo = -1
            powerOfTwo = 2
            factorOfThree = -1
            powerOfThree = 3
        } else if (value < 0.3) { // (0.2 - 0.25 - 0.3) -> (0.8 - 1.0 - 1.2)
            factorOfTwo = -2
            powerOfTwo = 4
        } else if (value < 0.42) { // (0.3 - 0.33333 - 0.42) -> (0.9 - 1.0 - 1.26)
            factorOfThree = -1
            powerOfThree = 3
        } else if (value < 0.7) { // (0.42 - 0.5 - 0.7) -> (0.84 - 1.0 - 1.4)
            factorOfTwo = -1
            powerOfTwo = 2
        } else if (value < 1.4) { // (0.7 - 1.0 - 1.4) -> (0.7 - 1.0 - 1.4)
            // do nothing
        } else if (value < 2.5) { // (1.4 - 2.0 - 2.5) -> (0.7 - 1.0 - 1.25)
            factorOfTwo = 1
            powerOfTwo = 2
        } else if (value < 3.5) { // (2.5 - 3.0 - 3.5) -> (0.833333 - 1.0 - 1.166667)
            factorOfThree = 1
            powerOfThree = 3
        } else if (value < 5.0) { // (3.5 - 4.0 - 5.0) -> (0.875 - 1.0 - 1.25)
            factorOfTwo = 2
            powerOfTwo = 4
        } else if (value < 7.0) { // (5.0 - 6.0 - 7.0) -> (0.833333 - 1.0 - 1.166667)
            factorOfThree = 1
            powerOfThree = 3
            factorOfTwo = 1
            powerOfTwo = 2
        } else if (value < 8.5) { // (7.0 - 8.0 - 8.5) -> (0.875 - 1.0 - 1.0625)
            factorOfTwo = 3
            powerOfTwo = 8
        } else if (value < 10.0) { // (8.5 - 9.0 - 10.0) -> (0.94444 - 1.0 - 1.11111)
            factorOfThree = 2
            powerOfThree = 9
        } else {
            while (value > 1.4) { // never happens when called by logUsingExponent()
                value /= 2.0
                factorOfTwo++
                powerOfTwo = powerOfTwo shl 1
            }
        }
        var correctedX = x
        var result = BigDecimal.ZERO
        if (factorOfTwo > 0) {
            correctedX = correctedX.divide(BigDecimal.valueOf(powerOfTwo.toLong()), mc)
            result = result.add(
                logTwo(mcDouble)!!
                    .multiply(BigDecimal.valueOf(factorOfTwo.toLong()), mc)
            )
        } else if (factorOfTwo < 0) {
            correctedX = correctedX.multiply(BigDecimal.valueOf(powerOfTwo.toLong()), mc)
            result = result.subtract(
                logTwo(mcDouble)!!
                    .multiply(BigDecimal.valueOf(-factorOfTwo.toLong()), mc)
            )
        }
        if (factorOfThree > 0) {
            correctedX = correctedX.divide(BigDecimal.valueOf(powerOfThree.toLong()), mc)
            result = result.add(
                logThree(mcDouble)!!
                    .multiply(BigDecimal.valueOf(factorOfThree.toLong()), mc)
            )
        } else if (factorOfThree < 0) {
            correctedX = correctedX.multiply(BigDecimal.valueOf(powerOfThree.toLong()), mc)
            result = result.subtract(
                logThree(mcDouble)!!
                    .multiply(BigDecimal.valueOf(-factorOfThree.toLong()), mc)
            )
        }
        if (x == correctedX && result == BigDecimal.ZERO) {
            return logUsingNewton(x, mathContext)
        }
        result = result.add(logUsingNewton(correctedX, mc), mc)
        return result
    }

    fun pi(mathContext: MathContext): BigDecimal? {
        checkMathContext(mathContext)
        var result: BigDecimal?
        synchronized(piCacheLock) {
            if (piCache != null && mathContext.precision <= piCache!!.precision()) {
                result = piCache
            } else {
                piCache = piChudnovski(mathContext)
                return piCache
            }
        }
        return round(result, mathContext)
    }

    private fun piChudnovski(mathContext: MathContext): BigDecimal {
        val mc = MathContext(mathContext.precision + 10, mathContext.roundingMode)
        val value24 = BigDecimal.valueOf(24)
        val value640320 = BigDecimal.valueOf(640320)
        val value13591409 = BigDecimal.valueOf(13591409)
        val value545140134 = BigDecimal.valueOf(545140134)
        val valueDivisor = value640320.pow(3).divide(value24, mc)
        var sumA = BigDecimal.ONE
        var sumB = BigDecimal.ZERO
        var a = BigDecimal.ONE
        var dividendTerm1: Long = 5 // -(6*k - 5)
        var dividendTerm2: Long = -1 // 2*k - 1
        var dividendTerm3: Long = -1 // 6*k - 1
        var kPower3 = BigDecimal.ZERO
        val iterationCount = ((mc.precision + 13) / 14).toLong()
        for (k in 1..iterationCount) {
            val valueK = BigDecimal.valueOf(k)
            dividendTerm1 += -6
            dividendTerm2 += 2
            dividendTerm3 += 6
            val dividend =
                BigDecimal.valueOf(dividendTerm1).multiply(BigDecimal.valueOf(dividendTerm2))
                    .multiply(
                        BigDecimal.valueOf(dividendTerm3)
                    )
            kPower3 = valueK.pow(3)
            val divisor = kPower3.multiply(valueDivisor, mc)
            a = a.multiply(dividend).divide(divisor, mc)
            val b = valueK.multiply(a, mc)
            sumA = sumA.add(a)
            sumB = sumB.add(b)
        }
        val value426880 = BigDecimal.valueOf(426880)
        val value10005 = BigDecimal.valueOf(10005)
        val factor = value426880.multiply(sqrt(value10005, mc))
        val pi = factor.divide(
            value13591409.multiply(sumA, mc).add(value545140134.multiply(sumB, mc)),
            mc
        )
        return round(pi, mathContext)
    }

    fun e(mathContext: MathContext): BigDecimal? {
        checkMathContext(mathContext)
        var result: BigDecimal?
        synchronized(eCacheLock) {
            if (eCache != null && mathContext.precision <= eCache!!.precision()) {
                result = eCache
            } else {
                eCache = exp(BigDecimal.ONE, mathContext)
                return eCache
            }
        }
        return round(result, mathContext)
    }

    private fun logTen(mathContext: MathContext): BigDecimal? {
        var result: BigDecimal? = null
        synchronized(log10CacheLock) {
            if (log10Cache != null && mathContext.precision <= log10Cache!!.precision()) {
                result = log10Cache
            } else {
                log10Cache = logUsingNewton(BigDecimal.TEN, mathContext)
                return log10Cache
            }
        }
        return round(result, mathContext)
    }

    private fun logTwo(mathContext: MathContext): BigDecimal? {
        var result: BigDecimal? = null
        synchronized(log2CacheLock) {
            if (log2Cache != null && mathContext.precision <= log2Cache!!.precision()) {
                result = log2Cache
            } else {
                log2Cache = logUsingNewton(TWO, mathContext)
                return log2Cache
            }
        }
        return round(result, mathContext)
    }

    private fun logThree(mathContext: MathContext): BigDecimal? {
        var result: BigDecimal?
        synchronized(log3CacheLock) {
            if (log3Cache != null && mathContext.precision <= log3Cache!!.precision()) {
                result = log3Cache
            } else {
                log3Cache = logUsingNewton(THREE, mathContext)
                return log3Cache
            }
        }
        return round(result, mathContext)
    }

    fun exp(x: BigDecimal, mathContext: MathContext): BigDecimal {
        checkMathContext(mathContext)
        return if (x.signum() == 0) {
            BigDecimal.ONE
        } else expIntegralFractional(x, mathContext)
    }

    private fun expIntegralFractional(x: BigDecimal, mathContext: MathContext): BigDecimal {
        val integralPart = integralPart(x)
        if (integralPart.signum() == 0) {
            return expTaylor(x, mathContext)
        }
        val fractionalPart = x.subtract(integralPart)
        val mc = MathContext(mathContext.precision + 10, mathContext.roundingMode)
        val z = BigDecimal.ONE.add(fractionalPart.divide(integralPart, mc))
        val t = expTaylor(z, mc)
        val result = pow(t, integralPart.intValueExact().toLong(), mc)
        return round(result, mathContext)
        //		return result;
    }

    private fun expTaylor(bigDecimal: BigDecimal, mathContext: MathContext): BigDecimal {
        var x = bigDecimal
        val mc = MathContext(mathContext.precision + 6, mathContext.roundingMode)
        x = x.divide(BigDecimal.valueOf(256), mc)
        var result = ExpCalculator.INSTANCE.calculate(x, mc)
        result = pow(result, 256, mc)
        return round(result, mathContext)
    }

    private fun checkMathContext(mathContext: MathContext) {
        if (mathContext.precision == 0) {
            throw UnsupportedOperationException("Unlimited MathContext not supported")
        }
    }

    init {
        var result = BigDecimal.ONE
        factorialCache[0] = result
        for (i in 1 until factorialCache.size) {
            result =
                result.multiply(
                    BigDecimal.valueOf(i.toLong())
                )
            factorialCache[i] = result
        }
    }
}