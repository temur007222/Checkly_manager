package com.temurx.checklymanager.ui

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.fragment.findNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.temurx.checklymanager.R
import com.temurx.checklymanager.data.Staff
import com.temurx.checklymanager.databinding.FragmentAddStaffBinding
import java.io.File
import java.util.UUID

class AddStaffFragment : Fragment() {

    private var _binding: FragmentAddStaffBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var photoUrl: String? = null   // store download URL after upload
    private var imageUri: Uri? = null    // used for camera capture

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && imageUri != null) {
                binding.profilePreview.setImageURI(imageUri)
                binding.profilePreview.visibility = View.VISIBLE
                uploadPhotoToStorage(imageUri)
            }
        }

        // Gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                binding.profilePreview.setImageURI(it)
                binding.profilePreview.visibility = View.VISIBLE
                uploadPhotoToStorage(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddStaffBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Photo upload tile click — primary picker entry point in the new layout
        binding.photoUploadGroup.setOnClickListener { showImagePickerDialog() }
        // Legacy hidden button (preserved for binding compat)
        binding.selectPhotoButton.setOnClickListener { showImagePickerDialog() }

        // Role dropdown adapter (works on minSdk 24+; XML simpleItems requires API 30)
        val roleAdapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.add_staff_roles)
        )
        binding.staffRoleInput.setAdapter(roleAdapter)

        binding.toolbar.setOnClickListener {
            findNavController().popBackStack()
        }

        // Save staff
        binding.saveButton.setOnClickListener {
            val name = binding.staffInputName.text.toString().trim()
            val role = binding.staffRoleInput.text.toString().trim()
            val email = binding.staffEmailInput.text.toString().trim()
            val password = binding.staffPasswordInput.text.toString().trim()

            if (name.isEmpty() || role.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.add_staff_validation), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val options = FirebaseApp.getInstance().options
            val secondaryApp = FirebaseApp.getApps(requireContext())
                .firstOrNull { it.name == "SecondaryApp" }
                ?: FirebaseApp.initializeApp(requireContext(), options, "SecondaryApp")

            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp!!)

            binding.saveButton.isEnabled = false
            Log.d("AddStaff", "creating user $email")

            secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        if (user != null) {
                            val staff = Staff(
                                staffId = user.uid,
                                fullName = name,
                                role = role,
                                photoUrl = photoUrl ?: "https://i.pinimg.com/1200x/79/9e/02/799e023c66397048a739096c6244ba4a.jpg",
                                totalTask = 0,
                                overdueCount = 0,
                                createdAt = Timestamp.now()
                            )
                            Log.d("AddStaff", "auth user created uid=${user.uid}; writing staff_list doc")

                            db.collection("staff_list").document(user.uid).set(staff)
                                .addOnSuccessListener {
                                    Log.d("AddStaff", "staff_list write success uid=${user.uid}")
                                    if (_binding != null) {
                                        Toast.makeText(requireContext(), getString(R.string.add_staff_added_toast), Toast.LENGTH_SHORT).show()
                                        findNavController().popBackStack()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("AddStaff", "staff_list write failed", e)
                                    if (_binding != null) {
                                        binding.saveButton.isEnabled = true
                                        Toast.makeText(requireContext(), getString(R.string.add_staff_error, e.message ?: ""), Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                        secondaryAuth.signOut()
                    } else {
                        Log.e("AddStaff", "auth create failed", task.exception)
                        if (_binding != null) {
                            binding.saveButton.isEnabled = true
                            Toast.makeText(requireContext(), getString(R.string.add_staff_error, task.exception?.message ?: ""), Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }

        // Cancel
        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf(
            getString(R.string.add_staff_photo_take),
            getString(R.string.add_staff_photo_gallery)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_staff_photo_picker_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun openCamera() {
        val photoFile = File.createTempFile("staff_photo_", ".jpg", requireContext().cacheDir)
        imageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", photoFile)
        imageUri?.let { cameraLauncher.launch(it) }
    }

    // Upload image to Firebase Storage
    private fun uploadPhotoToStorage(imageUri: Uri?) {
        if (imageUri == null) return

        val storageRef = FirebaseStorage.getInstance().reference
            .child("staff_photos/${UUID.randomUUID()}.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    photoUrl = uri.toString()
                    Toast.makeText(requireContext(), getString(R.string.add_staff_photo_uploaded), Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), getString(R.string.add_staff_photo_failed), Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
