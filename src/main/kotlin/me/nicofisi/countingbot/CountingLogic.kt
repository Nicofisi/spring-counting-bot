package me.nicofisi.countingbot

object CountingLogic {
    private val englishDigitNames =
        listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
    private val numberEmojis =
        listOf(
            "\u0030\u20E3",
            "\u0031\u20E3",
            "\u0032\u20E3",
            "\u0033\u20E3",
            "\u0034\u20E3",
            "\u0035\u20E3",
            "\u0036\u20E3",
            "\u0037\u20E3",
            "\u0038\u20E3",
            "\u0039\u20E3"
        )

    fun messageMatchesNumber(rawContent: String, expectedNumber: Int): Boolean {
        val split = rawContent.split(" ")

        val digits = mutableListOf<Int>()
        val expectedDigits = expectedNumber.toString().map { it.toString().toInt() }.toIntArray()

        fun digitsMatchExpected() = expectedDigits.contentEquals(digits.toIntArray())

        split.forEach { word ->
            // 123
            val wordInt = word.toIntOrNull()
            if (wordInt != null && wordInt >= 0) {
                digits.addAll(word.map { it.toString().toInt() })
                return@forEach
            }

            // one two three
            val digitFromName = englishDigitNames.indexOf(word.toLowerCase())
            if (digitFromName != -1) {
                digits.add(digitFromName)
                return@forEach
            }

            var wordRemainder = word

            // :one: :two: :three:
            val emojiDigits = mutableListOf<Int>()

            do {
                val digitFromEmoji = numberEmojis.indexOfFirst { emoji -> wordRemainder.startsWith(emoji) }

                if (digitFromEmoji == -1) {
                    if (emojiDigits.isEmpty())
                        return digitsMatchExpected()
                    else
                        return@forEach // ignoring whatever can already be in emojiDigits - yes, that's intended
                }

                emojiDigits.add(digitFromEmoji)

                wordRemainder = wordRemainder.drop(numberEmojis[digitFromEmoji].length)
            } while (wordRemainder.isNotEmpty())

            digits.addAll(emojiDigits)
        }
        return digitsMatchExpected()
    }
}
