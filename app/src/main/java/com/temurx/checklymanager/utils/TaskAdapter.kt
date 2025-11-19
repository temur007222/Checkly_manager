package com.temurx.checklymanager.utils

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.temurx.checklymanager.R
import com.temurx.checklymanager.data.Task
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TaskAdapter(
    private var taskList: List<Task>,
    private val onItemClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskTitle: TextView = itemView.findViewById(R.id.taskTitle)
        val taskDue: TextView = itemView.findViewById(R.id.taskDue)
        val taskStatusIcon: ImageView = itemView.findViewById(R.id.taskStatusIcon)
    }

    // ✅ Formatter for human-readable dates
    @RequiresApi(Build.VERSION_CODES.O)
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm") // Example: 16 Sep 2025, 14:30
            .withZone(ZoneId.systemDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        holder.taskTitle.text = task.title

        // ✅ Convert Timestamp → formatted string
        val dueTimeStr = task.dueTime?.toDate()?.toInstant()?.let {
            formatter.format(it)
        } ?: "No due date"

        holder.taskDue.text = "Due: $dueTimeStr"

        // ✅ Status icon
        if (task.isCompleted) {
            holder.taskStatusIcon.setImageResource(R.drawable.ic_check)
        } else {
            holder.taskStatusIcon.setImageResource(R.drawable.ic_pending)
        }

        holder.itemView.setOnClickListener {
            onItemClick(task)
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateTasks(newList: List<Task>) {
        taskList = newList
        notifyDataSetChanged()
    }
}

