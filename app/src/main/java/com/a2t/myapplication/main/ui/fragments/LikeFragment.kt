package com.a2t.myapplication.main.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.a2t.myapplication.R
import com.a2t.myapplication.common.model.DLAnimator
import com.a2t.myapplication.databinding.FragmentLikeBinding
import com.a2t.myapplication.main.ui.activity.MainActivity

class LikeFragment: Fragment() {
    private var _binding: FragmentLikeBinding? = null
    private val binding get() = _binding!!
    private lateinit var ma: MainActivity
    private lateinit var context: Context
    private lateinit var dlAnimator: DLAnimator
    private var isQrCodeOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLikeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ma = requireActivity() as MainActivity
        context = requireContext()
        dlAnimator = DLAnimator()

        // Восстанавливаем параметры
        if (savedInstanceState != null) isQrCodeOpen = savedInstanceState.getBoolean("isQrCodeOpen", false)

        // Если QR код был открыт
        if (isQrCodeOpen) binding.ivIcon.setImageResource(R.drawable.qr_code_512)

        binding.ivIcon.setOnClickListener {
            flipIcon(isQrCodeOpen)
        }

        binding.tvLike.setOnClickListener {
            closeFragment()
            val uriPP = context.resources.getString(R.string.app_reviews_link)
            val browserIntent = Intent(Intent.ACTION_VIEW, uriPP.toUri())
            startActivity(browserIntent)
        }

        binding.tvSend.setOnClickListener {
            closeFragment()
            val str = context.resources.getString(R.string.recommend) + context.resources.getString(R.string.app_link)
            val intent = Intent(Intent.ACTION_SEND)
            intent.setType("text/plain")
            intent.putExtra(Intent.EXTRA_TEXT, str)
            val chooserIntent = Intent.createChooser(intent, getString(R.string.mail_service))
            startActivity(chooserIntent)
        }

        binding.tvQr.setOnClickListener {
            flipIcon(isQrCodeOpen)
        }
    }

    private fun flipIcon(isQrOpen: Boolean) {
        var title = R.drawable.qr_code_512
        if (isQrOpen) title = R.drawable.ic_mylist
        isQrCodeOpen = !isQrOpen
        dlAnimator.flipPicture(binding.ivIcon, title)
    }

    private fun closeFragment() {
        ma.supportFragmentManager.popBackStack()
    }

    override fun onStart() {
        super.onStart()
        ma.mainBackPressedCallback.isEnabled = false
    }

    override fun onStop() {
        super.onStop()
        ma.mainBackPressedCallback.isEnabled = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isQrCodeOpen", isQrCodeOpen)
        super.onSaveInstanceState(outState)
    }
}