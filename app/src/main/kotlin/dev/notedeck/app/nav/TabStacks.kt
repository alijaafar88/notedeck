package dev.notedeck.app.nav

/** One back stack per tab, as plain lists. */
class TabStacks<T>(private val tabs: List<T>, roots: (Int) -> T) {
    val stacks: List<MutableList<T>> = tabs.indices.map { mutableListOf(roots(it)) }
    var selected: Int = 0
        private set

    val current: MutableList<T> get() = stacks[selected]

    fun select(index: Int) {
        selected = index
    }

    /** Pops within the tab, then goes to the first tab. False means Back should leave the app. */
    fun back(): Boolean = when {
        current.size > 1 -> {
            current.removeAt(current.lastIndex)
            true
        }
        selected != 0 -> {
            selected = 0
            true
        }
        else -> false
    }
}
