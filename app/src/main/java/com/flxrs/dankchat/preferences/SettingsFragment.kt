package com.flxrs.dankchat.preferences

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.LinearLayoutManager
import com.flxrs.dankchat.BuildConfig
import com.flxrs.dankchat.MainFragment
import com.flxrs.dankchat.R
import com.flxrs.dankchat.databinding.MultiEntryBottomsheetBinding
import com.flxrs.dankchat.preferences.multientry.MultiEntryAdapter
import com.flxrs.dankchat.preferences.multientry.MultiEntryItem
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.moshi.Moshi
import kotlinx.android.synthetic.main.settings_fragment.view.*

class SettingsFragment : PreferenceFragmentCompat() {

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(MultiEntryItem.Entry::class.java)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.settings_toolbar
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = getString(R.string.settings)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        findPreference<Preference>(getString(R.string.preference_about_key))
            ?.summary = getString(R.string.preference_about_summary, BuildConfig.VERSION_NAME)

        val isLoggedIn = DankChatPreferenceStore(requireContext()).isLoggedin()
        findPreference<Preference>(getString(R.string.preference_logout_key))?.apply {
            isEnabled = isLoggedIn
            setOnPreferenceClickListener {
                with(findNavController()) {
                    previousBackStackEntry?.savedStateHandle?.set(
                        MainFragment.LOGOUT_REQUEST_KEY,
                        true
                    )
                    navigateUp()
                }
                true
            }
        }
        findPreference<SwitchPreference>(getString(R.string.preference_dark_theme_key))?.apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                if (!isChecked) {
                    isChecked = true
                }
                isVisible = false
            }
            setOnPreferenceClickListener {
                setDarkMode(isChecked)
                true
            }
        }
        findPreference<Preference>(getString(R.string.preference_custom_mentions_key))?.apply {
            setOnPreferenceClickListener { showMultiEntryPreference(key, sharedPreferences) }
        }
        findPreference<Preference>(getString(R.string.preference_blacklist_key))?.apply {
            setOnPreferenceClickListener { showMultiEntryPreference(key, sharedPreferences) }
        }
        findPreference<SeekBarPreference>(getString(R.string.preference_font_size_key))?.apply {
            summary = getFontSizeSummary(value)
            setOnPreferenceChangeListener { _, _ ->
                summary = getFontSizeSummary(value)
                true
            }
        }
    }

    private fun getFontSizeSummary(value: Int): String {
        return when {
            value < 13 -> getString(R.string.preference_font_size_summary_very_small)
            value in 13..17 -> getString(R.string.preference_font_size_summary_small)
            value in 18..22 -> getString(R.string.preference_font_size_summary_large)
            else -> getString(R.string.preference_font_size_summary_very_large)
        }
    }

    private fun setDarkMode(darkMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            when {
                darkMode -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }

    private fun showMultiEntryPreference(
        key: String,
        sharedPreferences: SharedPreferences
    ): Boolean {
        val entryStringSet = sharedPreferences.getStringSet(key, emptySet()) ?: emptySet()
        val entries = entryStringSet.mapNotNull { adapter.fromJson(it) }.sortedBy { it.entry }
            .plus(MultiEntryItem.AddEntry)

        val entryAdapter = MultiEntryAdapter(entries.toMutableList())
        val binding =
            MultiEntryBottomsheetBinding.inflate(LayoutInflater.from(requireContext())).apply {
                multiEntryList.layoutManager = LinearLayoutManager(requireContext())
                multiEntryList.adapter = entryAdapter
                multiEntrySheet.updateLayoutParams {
                    height = (resources.displayMetrics.heightPixels * 0.6f).toInt()
                }
            }
        BottomSheetDialog(requireContext()).apply {
            setContentView(binding.root)
            setOnDismissListener {
                val stringSet = entryAdapter.entries
                    .filterIsInstance<MultiEntryItem.Entry>()
                    .filter { it.entry.isNotBlank() }
                    .map { adapter.toJson(it) }
                    .toSet()

                sharedPreferences.edit { putStringSet(key, stringSet) }
            }
            behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.6f).toInt()
            show()
        }
        return true
    }
}