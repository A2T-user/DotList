package com.a2t.myapplication.main.ui.fragments

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import com.a2t.myapplication.R
import com.a2t.myapplication.databinding.FragmentMainMenuBinding
import com.a2t.myapplication.description.ui.DescriptionActivity
import com.a2t.myapplication.main.ui.activity.MainActivity
import com.a2t.myapplication.utilities.AppHelper

class MainMenuFragment: Fragment() {
    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainMenuBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var version: String
        try {
            val pInfo: PackageInfo =
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            version = pInfo.versionName.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            version = ""
        }
        binding.tvVersion.text = getString(R.string.version, version)

        binding.tvDescription.setOnClickListener { v ->
            AppHelper.requestFocusInTouch(v)
            val intent = Intent(requireContext(), DescriptionActivity::class.java)
            startActivity(intent)
        }

        binding.tvSettings.setOnClickListener { v ->
            AppHelper.requestFocusInTouch(v)
            (requireActivity() as MainActivity).fragmentManager.beginTransaction().setTransition(TRANSIT_FRAGMENT_OPEN)
                .add(R.id.container_view, SettingsFragment())
                .addToBackStack("settingsFragment").commit()
        }

        binding.tvPrivacyPolicy.setOnClickListener {
            val uriPP = "https://s89577.see.ru"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriPP))
            startActivity(browserIntent)
        }

        binding.tvFeedback.setOnClickListener {
            val context = requireContext()
            val email = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            email.putExtra(Intent.EXTRA_EMAIL, arrayOf(context.resources.getString(R.string.e_mail)))
            email.putExtra(Intent.EXTRA_SUBJECT, context.resources.getString(R.string.app_name))
            context.startActivity(Intent.createChooser(email, context.resources.getString(R.string.mail)))
        }
    }


    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).mainBackPressedCallback.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        view?.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    parentFragmentManager.beginTransaction().remove(this@MainMenuFragment).commitAllowingStateLoss() // Закрытие фрагмента
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).mainBackPressedCallback.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}