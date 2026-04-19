package com.speedster.payload

class CalculatorEngine {
    var display = "0"
    private var operand1: Double? = null
    private var pendingOperator: String? = null
    private var shouldResetDisplay = false

    fun onNumber(num: String) {
        if (display == "0" || shouldResetDisplay) {
            display = num
            shouldResetDisplay = false
        } else {
            display += num
        }
    }

    fun onOperator(op: String) {
        operand1 = display.toDoubleOrNull()
        pendingOperator = op
        shouldResetDisplay = true
    }

    fun onCalculate() {
        val operand2 = display.toDoubleOrNull() ?: return
        val op1 = operand1 ?: return
        
        val result = when (pendingOperator) {
            "+" -> op1 + operand2
            "-" -> op1 - operand2
            "×" -> op1 * operand2
            "÷" -> if (operand2 != 0.0) op1 / operand2 else "Error"
            else -> return
        }

        display = result.toString().removeSuffix(".0")
        operand1 = null
        pendingOperator = null
        shouldResetDisplay = true
    }

    fun clear() {
        display = "0"
        operand1 = null
        pendingOperator = null
        shouldResetDisplay = false
    }
}