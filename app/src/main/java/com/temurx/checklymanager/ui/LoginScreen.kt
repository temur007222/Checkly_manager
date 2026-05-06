package com.temurx.checklymanager.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.fragment.findNavController
import com.temurx.checklymanager.R
import com.temurx.checklymanager.databinding.FragmentLoginScreenBinding

class LoginScreen : Fragment() {
    private var _binding: FragmentLoginScreenBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // If already logged in → go directly to staff list
        if (auth.currentUser != null) {
            navigateToStaffList()
        }

        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPw.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.auth_error_empty_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.loginButton.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    binding.loginButton.isEnabled = true
                    if (task.isSuccessful) {
                        navigateToStaffList()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.auth_error_failed, task.exception?.message ?: ""),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun navigateToStaffList() {
        findNavController().navigate(R.id.action_loginScreen_to_staffListFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
