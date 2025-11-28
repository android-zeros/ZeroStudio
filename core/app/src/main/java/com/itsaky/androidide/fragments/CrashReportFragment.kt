package com.itsaky.androidide.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.ClipboardUtils
import com.itsaky.androidide.activities.MainActivity
import com.itsaky.androidide.buildinfo.BuildInfo
import com.itsaky.androidide.databinding.LayoutCrashReportBinding
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.utils.BuildInfoUtils

/*
*2025.10.28  Update log: Added restart button, copy without report button, added close program button
*
*/
class CrashReportFragment : Fragment() {

  private var binding: LayoutCrashReportBinding? = null
  private var closeAppOnClick = true

  companion object {

    const val KEY_TITLE = "crash_title"
    const val KEY_MESSAGE = "crash_message"
    const val KEY_TRACE = "crash_trace"
    const val KEY_CLOSE_APP_ON_CLICK = "close_on_app_click"

    @JvmStatic
    fun newInstance(trace: String): CrashReportFragment {
      return newInstance(null, null, trace, true)
    }

    @JvmStatic
    fun newInstance(
      title: String?,
      message: String?,
      trace: String,
      closeAppOnClick: Boolean
    ): CrashReportFragment {
      val frag = CrashReportFragment()
      val args = Bundle().apply {
        putString(KEY_TRACE, trace)
        putBoolean(KEY_CLOSE_APP_ON_CLICK, closeAppOnClick)
        title?.let { putString(KEY_TITLE, it) }
        message?.let { putString(KEY_MESSAGE, it) }
      }
      frag.arguments = args
      return frag
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return LayoutCrashReportBinding.inflate(inflater, container, false).also { binding = it }.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val args = requireArguments()
    closeAppOnClick = args.getBoolean(KEY_CLOSE_APP_ON_CLICK)
    var title: String? = getString(R.string.msg_ide_crashed)
    var message: String? = getString(R.string.msg_report_crash)
    if (args.containsKey(KEY_TITLE)) {
      title = args.getString(KEY_TITLE)
    }

    if (args.containsKey(KEY_MESSAGE)) {
      message = args.getString(KEY_MESSAGE)
    }

    val trace: String = if (args.containsKey(KEY_TRACE)) {
      buildReportText(args.getString(KEY_TRACE))
    } else {
      "No stack trace was provided for the report"
    }

    binding!!.apply {
      crashTitle.text = title
      crashSubtitle.text = message
      logText.text = trace

      val report: String = trace
      
      // Original close button (top right)
      closeButton.setOnClickListener {
        if (closeAppOnClick) {
          requireActivity().finishAffinity()
        } else {
          requireActivity().finish()
        }
      }

      // New "Restart" button
      restartButton.setOnClickListener {
        val intent = Intent(requireActivity(), MainActivity::class.java).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        requireActivity().finishAffinity()
      }

      // New "Copy Log" button
      copyLogButton.setOnClickListener {
        ClipboardUtils.copyText("AndroidIDE CrashLog", report)
        Toast.makeText(requireContext(), R.string.msg_log_copied, Toast.LENGTH_SHORT).show()
      }

      // Original "Report" button, now named "Report Issue"
      reportIssueButton.setOnClickListener { reportTrace(report) }

      // New "Exit App" button
      exitButton.setOnClickListener {
        requireActivity().finishAffinity()
      }
    }
  }

  private fun reportTrace(report: String) {
    ClipboardUtils.copyText("AndroidIDE CrashLog", report)
    Toast.makeText(requireContext(), R.string.msg_log_copied_opening_browser, Toast.LENGTH_LONG).show()
    val url = BuildInfo.REPO_URL + "/issues/new?assignees=&labels=bug&template=bug_report.md&title=[Bug]"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
  }

  private fun buildReportText(trace: String?): String {
    return """
AndroidIDE Crash Report
${BuildInfoUtils.getBuildInfoHeader()}

Stacktrace:
$trace
    """
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding = null
  }
}