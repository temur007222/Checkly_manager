package com.temurx.checklymanager.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.temurx.checklymanager.R
import com.temurx.checklymanager.data.Staff
import com.temurx.checklymanager.utils.TaskAdapter
import com.temurx.checklymanager.data.Task
import com.temurx.checklymanager.databinding.FragmentStaffDetailBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

class StaffDetailsFragment : Fragment() {
    private var _binding: FragmentStaffDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var taskAdapter: TaskAdapter
    private var taskListener: ListenerRegistration? = null
    private var staffId: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy").withZone(ZoneId.systemDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        staffId = arguments?.getString("staffId") ?: "abc123"
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setOnClickListener {
            findNavController().popBackStack() // goes back to previous fragment
        }

        // RecyclerView setup
        taskAdapter = TaskAdapter(emptyList()) { selectedTask ->
            val bundle = Bundle().apply {
                putString("staffId", staffId)
                putString("taskId", selectedTask.id)
                putString("title", selectedTask.title)
                putString("description", selectedTask.description)
                putString("status", selectedTask.status)

                val startTimeStr = selectedTask.startTime?.toDate()?.time?.let {
                    formatter.format(Date(it).toInstant())
                } ?: ""
                putString("startTime", startTimeStr)

                // Convert Timestamp → String for navigation
                val dueTimeStr = try {
                    formatter.format(selectedTask.dueTime.toDate().toInstant())
                } catch (e: Exception) {
                    ""
                }
                putString("dueTime", dueTimeStr)
            }
            findNavController().navigate(R.id.taskDetailFragment, bundle)
        }

        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.tasksRecyclerView.adapter = taskAdapter

        // 🔹 Load staff details (name, role, profileUrl)
        staffId?.let { id ->
            db.collection("staff_list")
                .document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val staff = document.toObject(Staff::class.java)
                        staff?.let { it ->
                            binding.staffName.text = it.fullName

                            "${it.role} ${it.punctualityRate}%".also { binding.staffRole.text = it }

                            val createdAt = it.createdAt?.toDate()?.toInstant()?.let {
                                formatter.format(it)
                            } ?: "No date"

                            binding.createdAt.text = createdAt

                            if (it.photoUrl.isNotEmpty()) {
                                Glide.with(this)
                                    .load(it.photoUrl)
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .into(binding.profileImage)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error fetching staff details", e)
                }
        }

        // 🔹 Real-time Firestore listener for tasks
        staffId?.let { id ->
            taskListener = db.collection("staff_task")
                .document(id)
                .collection("tasks")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val tasks = snapshot.toObjects(Task::class.java)
                        taskAdapter.updateTasks(tasks)
                    }
                }
        }

        // FAB to add new task
        binding.addTaskFab.setOnClickListener {
            val bundle = Bundle().apply {
                putString("staffId", staffId ?: "staff123")
            }
            findNavController().navigate(R.id.addTaskFragment, bundle)
        }
    }
        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
            taskListener?.remove()
        }
}
