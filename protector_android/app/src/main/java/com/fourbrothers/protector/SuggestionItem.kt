package com.fourbrothers.protector

data class SuggestionItem(
    val label: String,
    val kind: Kind
) {
    enum class Kind { PROTECTOR, MODEL, BOTH }

    val kindLabel: String
        get() = when (kind) {
            Kind.PROTECTOR -> "Protector"
            Kind.MODEL -> "Model"
            Kind.BOTH -> "Both"
        }

    override fun toString(): String = label
}
