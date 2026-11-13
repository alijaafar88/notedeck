package dev.notedeck.domain

import kotlinx.coroutines.flow.Flow

data class DeckStats(val name: String, val cards: Int, val dueNow: Int, val reviewsThisWeek: Int, val againThisWeek: Int)

/** What the coach may look at: a read-only snapshot, taken on the phone and sent with the request. */
data class StudySnapshot(val dailyGoal: Int, val decks: List<DeckStats>, val reviewsPerDay: List<Int>)

/** A proposal. Nothing changes until the learner applies it. */
data class WeeklyPlan(val dailyGoal: Int, val focusDecks: List<String>, val reason: String)

class InvalidPlanException(message: String) : Exception(message)

object PlanRules {
    val GOAL_RANGE = 5..200
    const val MAX_FOCUS = 3

    /** Trusts nothing the model wrote: the goal must be sensible and every focus deck must be one of the learner's. */
    fun validate(plan: WeeklyPlan, snapshot: StudySnapshot): WeeklyPlan {
        if (plan.dailyGoal !in GOAL_RANGE) throw InvalidPlanException("The coach suggested a daily goal of ${plan.dailyGoal}, which isn't sensible.")
        val known = snapshot.decks.map { it.name }.toSet()
        val focus = plan.focusDecks.distinct().filter { it in known }
        if (focus.isEmpty() || focus.size > MAX_FOCUS) throw InvalidPlanException("The coach's plan didn't name your decks. Try again.")
        val reason = plan.reason.trim().take(400)
        if (reason.isEmpty()) throw InvalidPlanException("The coach didn't explain its plan. Try again.")
        return WeeklyPlan(plan.dailyGoal, focus, reason)
    }
}

/** Proposes a plan from a snapshot. The backend's agent implements it. */
fun interface Coach {
    suspend fun propose(snapshot: StudySnapshot): WeeklyPlan
}

/** The learner's statistics, read from the phone's own data. */
fun interface StatsSource {
    suspend fun snapshot(): StudySnapshot
}

/** Where an applied plan lives. Applying sets the daily goal and remembers the focus decks. */
interface PlanStore {
    val active: Flow<WeeklyPlan?>

    suspend fun apply(plan: WeeklyPlan)
}
