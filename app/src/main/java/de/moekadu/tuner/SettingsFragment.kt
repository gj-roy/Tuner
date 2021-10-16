/*
 * Copyright 2020 Michael Moessner
 *
 * This file is part of Tuner.
 *
 * Tuner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tuner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.moekadu.tuner

import android.app.AlertDialog
import android.content.res.Resources
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.TextView
import androidx.preference.*
import kotlin.math.pow
import kotlin.math.roundToInt

fun indexToWindowSize(index: Int): Int {
  return 2f.pow(7 + index).roundToInt()
}

fun indexToTolerance(index: Int): Int {
  return when(index) {
    0 -> 1
    1 -> 2
    2 -> 3
    3 -> 5
    4 -> 7
    5 -> 10
    6 -> 15
    7 -> 20
    else -> throw RuntimeException("Invalid index for tolerance")
  }
}

class SettingsFragment : PreferenceFragmentCompat() {

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.preferences, rootKey)
    setHasOptionsMenu(true)
  }

  override fun onPrepareOptionsMenu(menu : Menu) {
//        super.onPrepareOptionsMenu(menu)
    menu.findItem(R.id.action_settings)?.isVisible = false
    menu.findItem(R.id.action_instruments)?.isVisible = false
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

    val appearance = findPreference<ListPreference?>("appearance")
      ?: throw RuntimeException("No appearance preference")

    appearance.summaryProvider =
      Preference.SummaryProvider<ListPreference> { preference ->
        when (preference?.value) {
          "dark" -> getString(R.string.dark_appearance)
          "light" -> getString(R.string.light_appearance)
          else -> getString(R.string.system_appearance)
        }
      }

    appearance.setOnPreferenceChangeListener { _, _ ->
      val act = activity as MainActivity?
      act?.recreate()
      true
    }

    val screenOnPreference = findPreference<SwitchPreferenceCompat?>("screenon")
      ?: throw RuntimeException("No screenon preference")

    screenOnPreference.setOnPreferenceChangeListener { _, newValue ->
      val screenOn = newValue as Boolean
      if (screenOn)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      else
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      true
    }

    val a4Frequency = findPreference<EditTextPreference?>("a4_frequency")
    a4Frequency?.setOnBindEditTextListener { editText ->
      editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }
    a4Frequency?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
      getString(R.string.hertz_str, preference?.text ?: "440")
    }

    val tolerance = findPreference<SeekBarPreference>("tolerance_in_cents") ?: throw RuntimeException("No tolerance preference")
    tolerance.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = getString(R.string.tolerance_summary, indexToTolerance(newValue as Int))
      true
    }
    tolerance.summary = getString(R.string.tolerance_summary,  indexToTolerance(tolerance.value))

    val numMovingAverage = findPreference<SeekBarPreference>("num_moving_average") ?: throw RuntimeException("No num_moving_average preference")
    numMovingAverage.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = resources.getQuantityString(R.plurals.num_moving_average_summary, newValue as Int, newValue)
      true
    }
    numMovingAverage.summary = resources.getQuantityString(R.plurals.num_moving_average_summary, numMovingAverage.value, numMovingAverage.value)

    val windowingFunction = findPreference<ListPreference?>("windowing")
    windowingFunction?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

    val windowSize = findPreference<SeekBarPreference>("window_size") ?: throw RuntimeException("No window_size preference")
    windowSize.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = getWindowSizeSummary(newValue as Int)
      true
    }
    windowSize.summary = getWindowSizeSummary(windowSize.value)

    val overlap = findPreference<SeekBarPreference?>("overlap") ?: throw RuntimeException("No overlap preference")
    overlap.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = getString(R.string.percent, newValue as Int)
      true
    }
    overlap.summary = getString(R.string.percent, overlap.value)

    val pitchHistoryDuration = findPreference<SeekBarPreference>("pitch_history_duration") ?: throw RuntimeException("No pitch history duration preference")
    pitchHistoryDuration.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = getPitchHistoryDurationSummary(newValue as Int)
      true
    }
    pitchHistoryDuration.summary = getPitchHistoryDurationSummary(pitchHistoryDuration.value)

    val maxNoise = findPreference<SeekBarPreference>("max_noise") ?: throw RuntimeException("No max noise preference")
    maxNoise.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = getMaxNoiseSummary(newValue as Int)
      true
    }
    maxNoise.summary = getMaxNoiseSummary(maxNoise.value)

    val pitchHistoryNumFaultyValues = findPreference<SeekBarPreference>("pitch_history_num_faulty_values") ?: throw RuntimeException("No pitch history num fault values preference")
    pitchHistoryNumFaultyValues.setOnPreferenceChangeListener { preference, newValue ->
      preference.summary = resources.getQuantityString(
        R.plurals.pitch_history_num_faulty_values_summary, newValue as Int, newValue
      )
      true
    }
    pitchHistoryNumFaultyValues.summary = resources.getQuantityString(
      R.plurals.pitch_history_num_faulty_values_summary,
      pitchHistoryNumFaultyValues.value, pitchHistoryNumFaultyValues.value
    )

    val resetSettings = findPreference<Preference>("setdefault") ?: throw RuntimeException("No reset settings preference")

    resetSettings.onPreferenceClickListener = Preference.OnPreferenceClickListener {
      (activity as MainActivity?)?.let { act ->
        val builder = AlertDialog.Builder(act)
                        .setTitle(R.string.reset_settings_prompt)
                        .setPositiveButton(R.string.yes) { _, _ ->
                          val preferenceEditor = PreferenceManager.getDefaultSharedPreferences(act).edit()
                          preferenceEditor.clear()
                          PreferenceManager.setDefaultValues(act, R.xml.preferences, true)
                          preferenceEditor.apply()
                          act.recreate()
                        }
                        .setNegativeButton(R.string.no) { dialog, _ -> dialog?.cancel() }
                builder.show()
            }
            false
        }


    val aboutPreference = findPreference<Preference>("about") ?: throw RuntimeException("no about preference available")
    aboutPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
      val textView = TextView(context)
      textView.text = getString(R.string.about_message, BuildConfig.VERSION_NAME)
      val pad = (20f * Resources.getSystem().displayMetrics.density).toInt()
      textView.setPadding(pad, pad, pad, pad)
      context?.let { ctx ->
        val builder = AlertDialog.Builder(ctx)
          .setTitle(R.string.about)
          .setView(textView)
        builder.show()
      }
      false
    }
    return super.onCreateView(inflater, container, savedInstanceState)
  }

  private fun getWindowSizeSummary(windowSizeIndex: Int): String {
    val s = indexToWindowSize(windowSizeIndex)
    // the factor 2 in the next line is used since only one wave inside the window is not enough for
    // accurate frequency finding.
    return "$s " + getString(R.string.samples) + " (" + getString(R.string.minimum_frequency) + getString(R.string.hertz, 2 * 44100f / s) + ")"
  }

  private fun getPitchHistoryDurationSummary(percent: Int): String {
    val s = percentToPitchHistoryDuration(percent)
    return getString(R.string.seconds, s)
  }

  private fun getMaxNoiseSummary(percent: Int): String {
    return getString(R.string.max_noise_summary, percent)
  }
}
