package me.nicofisi.countingbot

import org.junit.Test
import org.junit.jupiter.api.Assertions

class CountingMatchTest {

    @Test
    fun countingMatchTest() {
        for (i in 0 until 100) {
            Assertions.assertTrue(CountingLogic.messageMatchesNumber(i.toString(), i))
        }

        listOf(
            Pair("1", 1),
            Pair("13", 13),
            Pair("1395 hello", 1395),
            Pair("1 3 9 6 hey", 1396),
            Pair("12 34 hey", 1234),
            Pair("one", 1),
            Pair("five three", 53),
            Pair("9 one 22 how are you?", 9122),
            Pair("\u0033\u20E3 \u0039\u20E3", 39),
            Pair("\u0034\u20E3\u0031\u20E3", 41),
            Pair("3 9 eight 35 \u0032\u20E3 three hey what's up", 3983523)
        ).forEach {
            Assertions.assertTrue(CountingLogic.messageMatchesNumber(it.first, it.second))
        }

        listOf(
            Pair("12 3 bye 4", 1234),
            Pair("1 two five huh nine cya", 1259),
            Pair("039", 39),
            Pair("zero one", 1),
            Pair("\u0030\u20E3 \u0037\u20E3", 7)
        ).forEach {
            Assertions.assertFalse(CountingLogic.messageMatchesNumber(it.first, it.second))
        }
    }
}