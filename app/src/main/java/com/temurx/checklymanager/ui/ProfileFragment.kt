package com.temurx.checklymanager.ui

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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
    private val auth = FirebaseAuth.getInstance()

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

        val versionName = try {
            requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) { "1.0" }
        binding.versionLabel.text = getString(R.string.profile_version, versionName)

        binding.btnLogOut.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.profile_signout_confirm_title)
                .setMessage(R.string.profile_signout_confirm_msg)
                .setPositiveButton(R.string.profile_signout) { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    findNavController().navigate(R.id.loginScreen)
                    findNavController().popBackStack(R.id.loginScreen, false)
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }

        binding.itemTheme.setOnClickListener { showThemePicker() }
        binding.itemLanguage.setOnClickListener { showLanguagePicker() }

        // Reflect current theme + language
        binding.themeValue.text = currentThemeLabel()
        binding.languageValue.text = currentLanguageLabel()

        loadUserProfile()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadUserProfile() {
        val currentUser = auth.currentUser ?: return

        // Sensible defaults for managers (no staff_list doc).
        binding.staffName.text = currentUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: getString(R.string.profile_role_owner)
        binding.staffRole.text = getString(R.string.profile_meta_role_restaurant,
            getString(R.string.profile_role_owner),
            getString(R.string.profile_restaurant_default))
        binding.createdAt.text = currentUser.email.orEmpty()

        // Try to enrich from staff_list — if present, override defaults.
        db.collection("staff_list").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val staff = document.toObject(Staff::class.java)
                    staff?.let {
                        if (it.fullName.isNotBlank()) binding.staffName.text = it.fullName
                        if (it.role.isNotBlank()) {
                            binding.staffRole.text = getString(R.string.profile_meta_role_restaurant,
                                it.role,
                                getString(R.string.profile_restaurant_default))
                        }
                        if (!it.photoUrl.isNullOrEmpty() && !it.photoUrl.contains("pinimg.com")) {
                            Glide.with(this)
                                .load(it.photoUrl)
                                .placeholder(R.drawable.bg_avatar_hero)
                                .circleCrop()
                                .into(binding.profileImage)
                        }
                    }
                }
                // No toast on missing doc — managers are expected not to have a staff_list entry.
            }
            .addOnFailureListener {
                // Silently keep the email-derived defaults; no scary toast.
            }
    }

    private fun showThemePicker() {
        val options = arrayOf(
            getString(R.string.profile_theme_light),
            getString(R.string.profile_theme_dark),
            getString(R.string.profile_theme_auto)
        )
        val checked = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> 0
            AppCompatDelegate.MODE_NIGHT_YES -> 1
            else -> 2
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.profile_group_theme)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                AppCompatDelegate.setDefaultNightMode(
                    when (which) {
                        0 -> AppCompatDelegate.MODE_NIGHT_NO
                        1 -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
                binding.themeValue.text = options[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun showLanguagePicker() {
        val options = arrayOf(
            getString(R.string.profile_lang_en),
            getString(R.string.profile_lang_ru),
            getString(R.string.profile_lang_uz)
        )
        val tags = arrayOf("en", "ru", "uz")
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val checked = tags.indexOfFirst { current.startsWith(it) }.takeIf { it >= 0 } ?: 2

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.profile_group_language)
            .setSingleChoiceItems(options, checked) { dialog, which ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags[which]))
                binding.languageValue.text = options[which]
                dialog.dismiss()
            }
            .show()
    }

    private fun currentThemeLabel(): String = when (AppCompatDelegate.getDefaultNightMode()) {
        AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.profile_theme_light)
        AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.profile_theme_dark)
        else -> getString(R.string.profile_theme_auto)
    }

    private fun currentLanguageLabel(): String {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return when {
            tag.startsWith("en") -> getString(R.string.profile_lang_en)
            tag.startsWith("ru") -> getString(R.string.profile_lang_ru)
            tag.startsWith("uz") -> getString(R.string.profile_lang_uz)
            else -> getString(R.string.profile_lang_uz)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
