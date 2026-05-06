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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.temurx.checklymanager.R
import com.temurx.checklymanager.data.Comment
import com.temurx.checklymanager.data.TaskStatus
import com.temurx.checklymanager.databinding.FragmentTaskDetailBinding
import com.temurx.checklymanager.utils.CommentsAdapter
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TaskDetailFragment : Fragment() {

    private var _binding: FragmentTaskDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var staffId: String? = null
    private var taskId: String? = null
    private lateinit var commentsAdapter: CommentsAdapter
    private var commentsListener: ListenerRegistration? = null

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

        taskId = arguments?.getString("taskId")
        staffId = arguments?.getString("staffId")

        binding.toolbar.setOnClickListener {
            findNavController().popBackStack()
        }

        commentsAdapter = CommentsAdapter()
        binding.commentRv.layoutManager = LinearLayoutManager(requireContext())
        binding.commentRv.adapter = commentsAdapter
        binding.commentRv.isNestedScrollingEnabled = false

        // Initial header (no count yet — listener will populate)
        binding.commentsHeader.text = getString(R.string.task_comments_h, 0)

        if (taskId != null && staffId != null) {
            loadTaskDetails(staffId!!, taskId!!)
            attachCommentsListener(staffId!!, taskId!!)
        }

        binding.taskEditBtn.setOnClickListener {
            val bundle = Bundle().apply {
                putString("staffId", staffId)
                putString("taskId", taskId)
            }
            findNavController().navigate(R.id.addTaskFragment, bundle)
        }

        binding.taskDeleteBtn.setOnClickListener {
            taskId?.let { id ->
                deleteTask(id)
            }
        }

        binding.sendComment.setOnClickListener { sendComment() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        commentsListener?.remove()
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
                if (doc.exists() && _binding != null) {
                    val title = doc.getString("title").orEmpty()
                    val description = doc.getString("description").orEmpty()
                    val rawStatus = doc.getString("status")
                    val canonical = TaskStatus.fromWire(rawStatus)

                    val startTime = doc.getTimestamp("startTime")?.toDate()?.let {
                        formatter.format(it.toInstant())
                    } ?: ""
                    val dueTime = doc.getTimestamp("dueTime")?.toDate()?.let {
                        formatter.format(it.toInstant())
                    } ?: ""

                    binding.taskTitle.text = title
                    binding.taskDescription.text = description
                    binding.startTime.text = startTime
                    binding.taskDueTime.text = dueTime

                    binding.taskStatus.text = when (canonical) {
                        TaskStatus.NOT_YET_AVAILABLE -> getString(R.string.task_status_not_yet)
                        TaskStatus.AVAILABLE -> getString(R.string.task_status_available)
                        TaskStatus.IN_PROGRESS -> getString(R.string.task_status_in_progress)
                        TaskStatus.FINISHED -> getString(R.string.task_status_finished)
                        TaskStatus.OVERDUE -> getString(R.string.task_status_overdue)
                        else -> ""
                    }
                    binding.taskStatus.setBackgroundResource(when (canonical) {
                        TaskStatus.AVAILABLE -> R.drawable.pill_status_available
                        TaskStatus.IN_PROGRESS -> R.drawable.pill_status_in_progress
                        TaskStatus.FINISHED -> R.drawable.pill_status_done
                        TaskStatus.OVERDUE -> R.drawable.pill_status_overdue
                        else -> R.drawable.pill_status_wait
                    })
                }
            }
            .addOnFailureListener { e ->
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error loading task: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun attachCommentsListener(staffId: String, taskId: String) {
        commentsListener = db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener
                if (e != null || snapshot == null) return@addSnapshotListener

                val comments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.copy(id = doc.id)
                }
                commentsAdapter.submitList(comments)
                binding.commentsHeader.text =
                    getString(R.string.task_comments_h, comments.size)
            }
    }

    private fun sendComment() {
        val staffId = staffId ?: return
        val taskId = taskId ?: return
        val text = binding.addCommentEt.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return

        val currentUser = FirebaseAuth.getInstance().currentUser
        val authorName = currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "Manager"

        val comment = Comment(
            id = "",
            text = text,
            authorId = currentUser?.uid.orEmpty(),
            authorName = authorName,
            createdAt = Timestamp.now()
        )

        binding.sendComment.isEnabled = false
        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId)
            .collection("comments")
            .add(comment)
            .addOnSuccessListener {
                if (_binding != null) {
                    binding.addCommentEt.setText("")
                    binding.sendComment.isEnabled = true
                }
            }
            .addOnFailureListener { e ->
                if (_binding != null) {
                    binding.sendComment.isEnabled = true
                    Toast.makeText(requireContext(),
                        getString(R.string.task_comment_send_error, e.message ?: ""),
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun deleteTask(taskId: String) {
        val staffId = staffId ?: return
        db.collection("staff_task")
            .document(staffId)
            .collection("tasks")
            .document(taskId)
            .delete()
            .addOnSuccessListener {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
            .addOnFailureListener { e ->
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
