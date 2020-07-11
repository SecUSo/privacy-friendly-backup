package org.secuso.privacyfriendlybackup.ui.application

class GroupList<G, C> {

    val positionMap = HashMap<Int, Int>()

    val groupList : MutableList<Pair<G,Long>> = ArrayList()
    val childList : MutableList<Pair<C,Long>> = ArrayList()

    fun addGroupWithChildren(group : G, children : Collection<C>) {
        val groupId = groupList.size.toLong().shl(32)
        groupList.add(group to groupId)

        for(child in children) {
            val childId = groupId or childList.size.toLong()
            childList.add(child to childId)
        }
    }

    fun get() {

    }

}