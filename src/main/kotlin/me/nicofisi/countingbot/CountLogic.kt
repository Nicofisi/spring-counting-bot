package me.nicofisi.countingbot

import java.lang.NumberFormatException

object CountLogic {
    fun messageMatchesNumber(rawContent: String, expectedNumber: Int): Boolean {
        val split = rawContent.split(" ")

        try {
            if (Integer.parseInt(split[0]) == expectedNumber)
                return true
        } catch (_: NumberFormatException) {

        }

        return false
    }
}
