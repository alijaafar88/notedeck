package dev.notedeck.app.nav

// TODO(c4): each tab keeps its own back stack. Back pops within a tab, then returns to the first tab.
class TabStacks<T>(tabs: List<T>) {
    fun back(): Boolean = false
}
