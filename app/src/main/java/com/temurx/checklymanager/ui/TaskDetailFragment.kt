package com.temurx.checklymanager.ui

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.temurx.checklymanager.R
import com.temurx.checklymanager.databinding.FragmentTaskDetailBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.apply

class TaskDetailFragment : Fragment() {

    private var _binding: FragmentTaskDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var staffId: String? = null
    private var taskId: String? = null // if not null → editing mode

    @RequiresApi(Build.VERSION_CODES.O)
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy").withZone(ZoneId.systemDefault())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setOnClickListener {
            findNavController().popBackStack()
        }

        if (taskId != null && staffId != null) {
            loadTaskDetails(staffId!!, taskId!!)
        }

        taskId = arguments?.getString("taskId")
        staffId = arguments?.getString("staffId")

        if (taskId != null && staffId != null) {
            loadTaskDetails(staffId!!, taskId!!)
        }

        binding.taskEditBtn.setOnClickListener {
            val bundle = Bundle().apply {
                putString("staffId", staffId)
                putString("taskId", taskId)
            }
            findNavController().navigate(R.id.addTaskFragment, bundle)
        }

        binding.taskDeleteBtn.setOnClickListener {
            deleteTask(taskId)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTaskDetails(staffId: String, taskId: String) {
        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val title = doc.getString("title") ?: ""
                    val description = doc.getString("description") ?: ""
                    val status = doc.getString("status") ?: ""

                    // Convert Timestamp to formatted string
                    val startTimeTimestamp = doc.getTimestamp("startTime")
                    val startTime = startTimeTimestamp?.toDate()?.let {
                        formatter.format(it.toInstant())
                    } ?: ""

                    val dueTimeTimestamp = doc.getTimestamp("dueTime")
                    val dueTime = dueTimeTimestamp?.toDate()?.let {
                        formatter.format(it.toInstant())
                    } ?: "No due time"

                    // Bind to UI
                    binding.taskTitle.text = title
                    binding.taskDescription.text = description
                    binding.startTime.text = startTime
                    binding.taskDueTime.text = dueTime
                    binding.taskStatus.text = status
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading task: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // DELETE
    private fun deleteTask(taskId: String?) {
        val staffId = staffId ?: return
        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId!!)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
