package com.uni.colabtasks.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import com.uni.colabtasks.MainActivity
import dagger.hilt.android.EntryPointAccessors
import java.util.Calendar

private data class WidgetTask(val title: String, val listIsShared: Boolean)

class MyTasksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        val dao = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        ).taskDao()
        val (start, end) = todayRange()
        provideContent {
            // Flow observable de Room: la composición se actualiza sola cuando cambian
            // las tareas (no depende solo de updateAll, que únicamente recompone).
            val tasksFlow = remember { dao.tasksDueBetweenFlow(start, end) }
            val tasks by tasksFlow.collectAsState(initial = emptyList())
            GlanceTheme {
                WidgetContent(tasks.map { WidgetTask(it.title, false) })
            }
        }
    }

    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return start to cal.timeInMillis
    }
}

@Composable
private fun WidgetContent(tasks: List<WidgetTask>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mis tareas de hoy",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = tasks.size.toString(),
                style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold)
            )
        }
        Spacer(GlanceModifier.height(8.dp))

        if (tasks.isEmpty()) {
            Text(
                text = "Sin tareas para hoy 🎉",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
            )
        } else {
            LazyColumn {
                items(tasks) { task ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "•  ",
                            style = TextStyle(color = ColorProvider(Color(0xFFFF7A2F)))
                        )
                        Text(
                            text = task.title,
                            style = TextStyle(color = GlanceTheme.colors.onSurface),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

class MyTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyTasksWidget()
}
