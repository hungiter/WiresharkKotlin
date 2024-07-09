package com.example.ptnet.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.ptnet.R

class SettingsFragment {
    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            setPreferencesFromResource(R.xml.dns_preferences, rootKey)
            setPreferencesFromResource(R.xml.geoip_preferences, rootKey)
            setPreferencesFromResource(R.xml.socks5_preferences, rootKey)
        }
    }
}