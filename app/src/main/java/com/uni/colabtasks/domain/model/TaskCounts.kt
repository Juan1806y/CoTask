package com.uni.colabtasks.domain.model

data class TaskCounts(
    val total: Int,
    val pending: Int,
    val done: Int
) {
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
    val progressPercent: Int get() = (progress * 100).toInt()

    companion object {
        val Empty = TaskCounts(0, 0, 0)
    }
}
