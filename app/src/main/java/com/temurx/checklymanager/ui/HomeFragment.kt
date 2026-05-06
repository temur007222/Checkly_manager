package com.temurx.checklymanager.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.temurx.checklymanager.R
import com.temurx.checklymanager.data.Staff
import com.temurx.checklymanager.data.Task
import com.temurx.checklymanager.databinding.FragmentHomeBinding
import com.temurx.checklymanager.utils.StaffAdapter
import java.time.Instant

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: StaffAdapter
    private val db = FirebaseFirestore.getInstance()
    private val staffList = mutableListOf<Staff>()

    private var staffListener: ListenerRegistration? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        adapter = StaffAdapter(staffList) { staff ->
            val bundle = Bundle().apply { putString("staffId", staff.staffId) }
            findNavController().navigate(R.id.staffDetailFragment, bundle)
        }

        binding.staffRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.staffRecycler.adapter = adapter

        binding.addStaffFab.setOnClickListener {
            findNavController().navigate(R.id.addStaffFragment)
        }

        fetchStaff()

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchStaff() {
        binding.progressBar.visibility = View.VISIBLE

        staffListener = db.collection("staff_list")
            .addSnapshotListener { snapshot, e ->
                if (_binding == null) return@addSnapshotListener  // safe check
                binding.progressBar.visibility = View.GONE

                if (e != null) {
                    Log.e("Firestore", "Error fetching staff: ", e)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    staffList.clear()
                    binding.heroCount.text = getString(R.string.home_count, snapshot.size())

                    for (doc in snapshot.documents) {
                        val staff = doc.toObject(Staff::class.java)?.copy(staffId = doc.id)
                        if (staff != null) {
                            // Fetch tasks for this staff
                            db.collection("staff_task")
                                .document(staff.staffId)
                                .collection("tasks")
                                .get()
                                .addOnSuccessListener { tasksSnapshot ->
                                    if (_binding == null) return@addOnSuccessListener

                                    val tasks = tasksSnapshot.toObjects(Task::class.java)
                                    val now = System.currentTimeMillis()

                                    staff.totalTask = tasks.size
                                    staff.overdueCount = tasks.count { task ->
                                        val dueTimeMillis = try {
                                            task.dueTime.toDate().time
                                        } catch (ex: Exception) {
                                            0L
                                        }
                                        !task.isCompleted && dueTimeMillis < now
                                    }
                                    staffList.add(staff)
                                    adapter.notifyDataSetChanged()
                                }
                                .addOnFailureListener { ex ->
                                    Log.e(
                                        "Firestore",
                                        "Error loading tasks for ${staff.fullName}",
                                        ex
                                    )
                                }
                        }
                    }
                } else {
                    Log.w("Firestore", "No staff data found")
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        staffListener?.remove()
    }
}
