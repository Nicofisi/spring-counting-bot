package me.nicofisi.countingbot.achievements

import me.nicofisi.countingbot.ApplicationContextGetter
import me.nicofisi.countingbot.data.CChannel
import me.nicofisi.countingbot.data.CCountInfoId
import me.nicofisi.countingbot.data.CUser
import me.nicofisi.countingbot.data.CountInfoRepository
import org.springframework.data.repository.findByIdOrNull
import java.sql.Date
import java.time.LocalDate

enum class Achievement(val id: Int, val checker: AchievementChecker?, val description: String) {
    FIRST_COUNT(
        1,
        FirstXCountsChecker(1),
        "Count for the first time"
    ),
    FIRST_10_COUNTS(
        2,
        FirstXCountsChecker(10),
        "Count 10 times in total"
    ),
    FIRST_100_COUNTS(
        3,
        FirstXCountsChecker(100),
        "Count 100 times in total"
    ),
    FIRST_1000_COUNTS(
        4,
        FirstXCountsChecker(1000),
        "Count 1000 times in total"
    ),
    FIRST_10000_COUNTS(
        5,
        FirstXCountsChecker(10000),
        "Count 10000 times in total"
    ),
    COUNT_5_TIMES_FOR_3_DAYS_IN_ROW(
        6,
        XForXDaysInRowChecker(5, 3),
        "Count at least 5 times for 3 days in a row"
    ),
    COUNT_5_TIMES_FOR_5_DAYS_IN_ROW(
        7,
        XForXDaysInRowChecker(5, 5),
        "Count at least 5 times for 5 days in a row"
    ),
    COUNT_5_TIMES_FOR_7_DAYS_IN_ROW(
        8,
        XForXDaysInRowChecker(5, 7),
        "Count at least 5 times for 7 days in a row"
    ),
    COUNT_5_TIMES_FOR_14_DAYS_IN_ROW(
        9,
        XForXDaysInRowChecker(5, 14),
        "Count at least 5 times for 14 days in a row"
    ),
    COUNT_5_TIMES_FOR_21_DAYS_IN_ROW(
        10,
        XForXDaysInRowChecker(5, 21),
        "Count at least 5 times for 21 days in a row"
    ),
    COUNT_5_TIMES_FOR_28_DAYS_IN_ROW(
        11,
        XForXDaysInRowChecker(5, 28),
        "Count at least 5 times for 28 days in a row"
    ),
    COUNT_5_TIMES_FOR_3_DAYS_IN_TOTAL(
        12,
        XForXDaysInTotalChecker(5, 3),
        "Count at least 5 times for 3 days in total"
    ),
    COUNT_5_TIMES_FOR_5_DAYS_IN_TOTAL(
        13,
        XForXDaysInTotalChecker(5, 5),
        "Count at least 5 times for 5 days in total"
    ),
    COUNT_5_TIMES_FOR_7_DAYS_IN_TOTAL(
        14,
        XForXDaysInTotalChecker(5, 7),
        "Count at least 5 times for 7 days in total"
    ),
    COUNT_5_TIMES_FOR_14_DAYS_IN_TOTAL(
        15,
        XForXDaysInTotalChecker(5, 14),
        "Count at least 5 times for 14 days in total"
    ),
    COUNT_5_TIMES_FOR_21_DAYS_IN_TOTAL(
        16,
        XForXDaysInTotalChecker(5, 21),
        "Count at least 5 times for 21 days in total"
    ),
    COUNT_5_TIMES_FOR_28_DAYS_IN_TOTAL(
        17,
        XForXDaysInTotalChecker(5, 28),
        "Count at least 5 times for 28 days in total"
    ),
    COUNT_5_TIMES_IN_A_DAY(
        18,
        XTimesInDayChecker(5),
        "Count 5 times within one day"
    ),
    COUNT_25_TIMES_IN_A_DAY(
        19,
        XTimesInDayChecker(25),
        "Count 25 times within one day"
    ),
    COUNT_50_TIMES_IN_A_DAY(
        20,
        XTimesInDayChecker(50),
        "Count 50 times within one day"
    ),
    COUNT_150_TIMES_IN_A_DAY(
        21,
        XTimesInDayChecker(150),
        "Count 150 times within one day"
    ),
    COUNT_300_TIMES_IN_A_DAY(
        22,
        XTimesInDayChecker(300),
        "Count 300 times within one day"
    ),
    BE_IN_THE_DAILY_TOP_3(
        23,
        null,
        "When the day ends, be in the daily top 3"
    ),
    BE_IN_THE_WEEKLY_TOP_5(
        24,
        null,
        "When the week ends, be in the daily top 5"
    ),
    BE_THE_DAILY_BEST_COUNTER(
        25,
        null,
        "When the day ends, be the top 1 counter"
    ),
    CHECK_STATS_FOR_THE_FIRST_TIME(
        26,
        null,
        "Use the ranking command for the first time"
    )

}

interface AchievementChecker {
    fun check(user: CUser, channel: CChannel): Boolean
}

data class FirstXCountsChecker(val counts: Int) : AchievementChecker {
    private var countInfoRepository = ApplicationContextGetter.context.getBean(CountInfoRepository::class.java)

    override fun check(user: CUser, channel: CChannel): Boolean {
        return countInfoRepository.getCountSumByChannelIdAndUserId(channel.id, user.id) >= counts
    }
}

data class XForXDaysInRowChecker(val counts: Int, val days: Int) :
    AchievementChecker {
    private var countInfoRepository = ApplicationContextGetter.context.getBean(CountInfoRepository::class.java)

    override fun check(user: CUser, channel: CChannel): Boolean {
        return (0 until days).map { LocalDate.now().minusDays(it.toLong()) }.all { localDate ->
            countInfoRepository
                .findByIdOrNull(
                    CCountInfoId(
                        user.id,
                        channel.id,
                        Date.valueOf(localDate)
                    )
                )
                ?.amount ?: 0 >= counts
        }
    }
}

data class XForXDaysInTotalChecker(val counts: Int, val days: Int) :
    AchievementChecker {
    private var countInfoRepository = ApplicationContextGetter.context.getBean(CountInfoRepository::class.java)

    override fun check(user: CUser, channel: CChannel): Boolean {
        return countInfoRepository.getAllByIdChannelIdAndIdUserId(channel.id, user.id)
            .filter { it.amount >= counts }.size >= days
    }
}

data class XTimesInDayChecker(val counts: Int) : AchievementChecker {
    private var countInfoRepository = ApplicationContextGetter.context.getBean(CountInfoRepository::class.java)

    override fun check(user: CUser, channel: CChannel): Boolean {
        return countInfoRepository
            .findByIdOrNull(
                CCountInfoId(
                    user.id,
                    channel.id,
                    Date.valueOf(LocalDate.now())
                )
            )?.amount ?: 0 >= counts
    }
}