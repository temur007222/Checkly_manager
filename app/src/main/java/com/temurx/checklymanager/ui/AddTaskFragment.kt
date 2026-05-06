package com.temurx.checklymanager.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.temurx.checklymanager.R
import com.temurx.checklymanager.databinding.FragmentAddTaskBinding
import com.temurx.checklymanager.data.Task
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.ExperimentalTime

class AddTaskFragment : Fragment() {

    private var _binding: FragmentAddTaskBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    private var staffId: String? = null
    private var taskId: String? = null // if not null → editing mode

    @RequiresApi(Build.VERSION_CODES.O)
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy").withZone(ZoneId.systemDefault())

    private var selectedDueTime: Timestamp? = null // keep selected time here
    private var selectedStartTime: Timestamp? = null // keep selected time here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        staffId = arguments?.getString("staffId")
        taskId = arguments?.getString("taskId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Toolbar back (toolbar is an ImageView in the redesigned layout)
        binding.toolbar.setOnClickListener {
            findNavController().popBackStack()
        }

        // StartTime picker
        binding.startTimeInput.setOnClickListener { showDateTimePicker(isStart = true) }

        // DateTime picker
        binding.dueTimeInput.setOnClickListener { showDateTimePicker(isStart = false) }

        // Cancel button
        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Save button
        binding.saveButton.setOnClickListener {
            if (taskId == null) saveNewTask() else updateTask(taskId!!)
        }

        // Load task if editing
        if (taskId != null) {
            loadTaskData(taskId!!)
        }
    }

    // CREATE
    @OptIn(ExperimentalTime::class)
    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveNewTask() {
        val title = binding.taskTitleInput.text.toString().trim()
        val notes = binding.notesInput.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.add_task_validation_title), Toast.LENGTH_SHORT).show()
            return
        }

        if ( selectedStartTime == null) {
            Toast.makeText(requireContext(), getString(R.string.add_task_validation_start), Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDueTime == null) {
            Toast.makeText(requireContext(), getString(R.string.add_task_validation_due), Toast.LENGTH_SHORT).show()
            return
        }

        val staffId = staffId ?: return
        val taskDocRef = db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document()

        val taskId = taskDocRef.id

        val requiresPhoto = binding.photoRequiredCheckbox.isChecked

        val task = Task(
            id = taskId,
            title = title,
            description = notes,
            startTime = selectedStartTime!!,
            dueTime = selectedDueTime!!, // Timestamp
            createdAt = Timestamp.now(),
            completedAt = null,
            isCompleted = false,
            photoUrls = emptyList(),
            requiresPhoto = requiresPhoto,
            createdBy = "Manager"
        )

        taskDocRef.set(task)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.add_task_added_toast), Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), getString(R.string.add_task_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    // UPDATE
    private fun updateTask(taskId: String) {
        val staffId = staffId ?: return
        val updates = mutableMapOf<String, Any>(
            "title" to binding.taskTitleInput.text.toString().trim(),
            "description" to binding.notesInput.text.toString().trim(),
            "requiresPhoto" to binding.photoRequiredCheckbox.isChecked
        )
        selectedStartTime?.let { updates ["startTime"] = it }
        selectedDueTime?.let { updates["dueTime"] = it }

        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.add_task_updated_toast), Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), getString(R.string.add_task_error, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    // LOAD EXISTING DATA
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTaskData(taskId: String) {
        val staffId = staffId ?: return
        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val task = doc.toObject(Task::class.java)
                    task?.let {
                        binding.taskTitleInput.setText(it.title)
                        binding.notesInput.setText(it.description)
                        selectedStartTime = it.startTime
                        binding.startTimeInput.setText(
                            formatter.format(it.startTime?.toDate()?.toInstant())
                        )
                        selectedDueTime = it.dueTime
                        binding.dueTimeInput.setText(
                            formatter.format(it.dueTime.toDate().toInstant())
                        )
                        binding.photoRequiredCheckbox.isChecked = it.requiresPhoto
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // PICK DATE & TIME
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDateTimePicker(isStart: Boolean) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val timePicker = TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        calendar.set(year, month, dayOfMonth, hour, minute)

                        val instant = calendar.time.toInstant()
                        val timestamp = Timestamp(Date.from(instant))
                        val formatted = formatter.format(instant)

                        if (isStart) {
                            selectedStartTime = timestamp
                            binding.startTimeInput.setText(formatted)
                        } else {
                            selectedDueTime = timestamp
                            binding.dueTimeInput.setText(formatted)
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePicker.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(staffId: String, taskId: String? = null) = AddTaskFragment().apply {
            arguments = Bundle().apply {
                putString("staffId", staffId)
                taskId?.let { putString("taskId", it) }
            }
        }
    }
}
