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
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.temurx.checklymanager.R
import com.temurx.checklymanager.data.Staff
import com.temurx.checklymanager.databinding.FragmentProfileBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // Firebase Auth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Log out user

            // Navigate to login screen
            findNavController().navigate(R.id.loginScreen)

            // Optionally, clear back stack to prevent "back" to Profile/Dashboard
            findNavController().popBackStack(R.id.loginScreen, false)
        }
        loadUserProfile()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "No logged-in user", Toast.LENGTH_SHORT).show()
            return
        }

        // Assuming you store users in collection "staff_list" with document ID = UID
        db.collection("staff_list").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val staff = document.toObject(Staff::class.java)
                    staff?.let {
                        binding.staffName.text = it.fullName ?: "Unknown"
                        binding.staffRole.text = it.role ?: "Unknown"
                        binding.createdAt.text = it.createdAt?.let { date ->
                            // Assuming createdAt is a Timestamp
                            val localDate = date.toDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            DateTimeFormatter.ofPattern("dd.MM.yyyy").format(localDate)
                        } ?: "Unknown"

                        // Load profile image if URL exists
                        if (!it.photoUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(it.photoUrl)
                                .placeholder(R.drawable.ic_launcher_background)
                                .circleCrop()
                                .into(binding.profileImage)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
