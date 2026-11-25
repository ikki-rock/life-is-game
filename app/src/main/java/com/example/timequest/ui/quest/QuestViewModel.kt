package com.example.timequest.ui.quest

import androidx.lifecycle.ViewModel
import com.example.timequest.data.Difficulty
import com.example.timequest.data.Quest
import com.example.timequest.data.QuestReward
import com.example.timequest.data.Store

class QuestViewModel(private val store: Store) : ViewModel() {
    val quests = store.quests
    val progress = store.progress

    fun add(title: String, difficulty: Difficulty): Boolean = store.addQuest(title, difficulty) != null

    fun update(id: String, title: String, difficulty: Difficulty) = store.updateQuest(id, title, difficulty)

    fun setDone(id: String, done: Boolean): QuestReward? = store.setDone(id, done)

    /** 삭제하고, 되돌리기에 쓸 (퀘스트, 위치)를 반환한다. */
    fun delete(id: String): Pair<Quest, Int>? {
        val list = store.quests.value
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return null
        val quest = list[index]
        store.deleteQuest(id)
        return quest to index
    }

    fun restore(quest: Quest, index: Int) = store.restoreQuest(quest, index)

    fun clearCompleted(): Int = store.clearCompleted()
}
