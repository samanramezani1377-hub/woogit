package com.samanramezani1377.woogit.presentation.ai

import java.math.BigDecimal
import java.math.RoundingMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure local calculator for Agent arithmetic. It never calls WooCommerce or executes code.
 * Supports scalar expressions and independent batched expressions (useful for product lists).
 */
internal class AiCalculatorToolExecutor {
    fun execute(args: JSONObject): String {
        val calculations = args.optJSONArray("calculations")
        if (calculations != null) return executeBatch(calculations)

        val expression = args.optString("expression").trim()
        if (expression.isBlank()) return failure("expression لازم است.")
        return runCatching {
            JSONObject().put("ok", true)
                .put("expression", expression)
                .put("result", format(evaluate(expression)))
                .toString()
        }.getOrElse { failure(it.message ?: "محاسبه ناموفق بود.") }
    }

    private fun executeBatch(calculations: JSONArray): String {
        if (calculations.length() == 0) return failure("calculations نباید خالی باشد.")
        if (calculations.length() > MAX_BATCH) return failure("حداکثر $MAX_BATCH محاسبه در هر فراخوانی مجاز است.")

        val results = JSONArray()
        for (index in 0 until calculations.length()) {
            val item = calculations.optJSONObject(index) ?: return failure("آیتم شماره ${index + 1} معتبر نیست.")
            val id = item.optString("id").trim()
            val expression = item.optString("expression").trim()
            if (id.isBlank()) return failure("شناسه محاسبه شماره ${index + 1} لازم است.")
            if (expression.isBlank()) return failure("expression محاسبه $id لازم است.")

            runCatching { evaluate(expression) }.onSuccess { value ->
                results.put(JSONObject().put("id", id).put("expression", expression).put("result", format(value)))
            }.onFailure {
                results.put(JSONObject().put("id", id).put("expression", expression).put("ok", false)
                    .put("error", it.message ?: "محاسبه ناموفق بود."))
            }
        }

        return JSONObject().put("ok", true).put("count", results.length()).put("data", results).toString()
    }

    private fun evaluate(expression: String): BigDecimal {
        require(expression.length <= MAX_EXPRESSION_LENGTH) { "عبارت محاسباتی بیش از حد طولانی است." }
        val parser = Parser(expression)
        val result = parser.parseExpression()
        parser.skipWhitespace()
        require(parser.atEnd()) { "عبارت محاسباتی نامعتبر است." }
        return result.stripTrailingZeros()
    }

    private fun format(value: BigDecimal): String = value.setScale(MAX_SCALE, RoundingMode.HALF_UP)
        .stripTrailingZeros().toPlainString()

    private fun failure(message: String) = JSONObject().put("ok", false).put("error", message).toString()

    private class Parser(private val input: String) {
        private var position = 0

        fun parseExpression(): BigDecimal {
            var value = parseTerm()
            while (true) {
                skipWhitespace()
                value = when {
                    consume('+') -> value.add(parseTerm())
                    consume('-') -> value.subtract(parseTerm())
                    else -> return value
                }
            }
        }

        private fun parseTerm(): BigDecimal {
            var value = parseFactor()
            while (true) {
                skipWhitespace()
                value = when {
                    consume('*') -> value.multiply(parseFactor())
                    consume('/') -> value.divide(parseFactor(), MAX_SCALE, RoundingMode.HALF_UP)
                    else -> return value
                }
            }
        }

        private fun parseFactor(): BigDecimal {
            skipWhitespace()
            if (consume('+')) return parseFactor()
            if (consume('-')) return parseFactor().negate()
            if (consume('(')) {
                val value = parseExpression()
                require(consume(')')) { "پرانتز بسته نشده است." }
                return value
            }

            val start = position
            var dotSeen = false
            var digitSeen = false
            while (position < input.length) {
                val char = input[position]
                when {
                    char.isDigit() -> { digitSeen = true; position++ }
                    char == '.' && !dotSeen -> { dotSeen = true; position++ }
                    else -> break
                }
            }
            require(digitSeen) { "عدد معتبر در موقعیت $start پیدا نشد." }
            return input.substring(start, position).toBigDecimal()
        }

        fun skipWhitespace() { while (position < input.length && input[position].isWhitespace()) position++ }
        fun atEnd(): Boolean = position >= input.length

        private fun consume(expected: Char): Boolean {
            if (position < input.length && input[position] == expected) {
                position++
                return true
            }
            return false
        }
    }

    private companion object {
        const val MAX_BATCH = 100
        const val MAX_EXPRESSION_LENGTH = 512
        const val MAX_SCALE = 12
    }
}
