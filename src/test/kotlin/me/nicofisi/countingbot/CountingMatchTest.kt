package me.nicofisi.countingbot

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class CountingMatchTest {

    @TestFactory
    fun countingMatchTest() =
        ((0 until 100).map { Pair(it.toString(), it) } + listOf(
            Pair("1", 1),
            Pair("13", 13),
            Pair("1395 hello", 1395),
            Pair("1 3 9 6 hey", 1396),
            Pair("12 34 hey", 1234),
            Pair("one", 1),
            Pair("five three", 53),
            Pair("9 one 22 how are you?", 9122),
            Pair("\u0033\u20E3 \u0039\u20E3", 39), // :three: :nine:
            Pair("\u0034\u20E3\u0031\u20E3", 41), // :four::one:
            Pair("3 9 eight 35 \u0032\u20E3 three hey what's up", 3983523) // 3 9 eight 35 :two: three
        )).map {
            DynamicTest.dynamicTest("Input ${it.first} should match number ${it.second}") {
                Assertions.assertTrue(CountingLogic.messageMatchesNumber(it.first, it.second))
            }
        }

    @TestFactory
    fun countingNotMatchTest() =
        listOf(
            Pair("12 3 bye 4", 1234),
            Pair("1 two five huh nine cya", 1259),
            Pair("039", 39),
            Pair("zero one", 1),
            Pair("\u0030\u20E3 \u0037\u20E3", 7),
            Pair("-3", 3),
            Pair("7 -4 one", 741),
            Pair("3-41", 341)
        ).map {
            DynamicTest.dynamicTest("Input ${it.first} shouldn't match number ${it.second}") {
                Assertions.assertFalse(CountingLogic.messageMatchesNumber(it.first, it.second))
            }
        }
}