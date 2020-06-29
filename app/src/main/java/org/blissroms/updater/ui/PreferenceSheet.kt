package org.blissroms.updater.ui

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemProperties
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import org.blissroms.updater.R
import org.blissroms.updater.UpdatesActivity
import org.blissroms.updater.UpdateImporter
import org.blissroms.updater.UpdatesCheckReceiver
import org.blissroms.updater.UpdateView
import org.blissroms.updater.controller.UpdaterController
import org.blissroms.updater.controller.UpdaterService
import org.blissroms.updater.misc.Constants
import org.blissroms.updater.misc.Utils
import org.blissroms.updater.model.Update
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@SuppressLint("UseSwitchCompatOrMaterialCode")
class PreferenceSheet : BottomSheetDialogFragment(), UpdateImporter.Callbacks {

    private var prefs: SharedPreferences? = null

    private var mUpdaterService: UpdaterService? = null

    private var mDownloadIds: MutableList<String>? = null

    private lateinit var mUpdateImporter: UpdateImporter

    private lateinit var updatesActivity: UpdatesActivity

    private lateinit var updateView: UpdateView

    private lateinit var preferencesAutoDeleteUpdates: Switch
    private lateinit var preferencesMeteredNetworkWarning: Switch
    private lateinit var preferencesAutoUpdatesCheckInterval: Spinner
    private lateinit var buttonLocalUpdate: Button

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.preferences_dialog, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatesActivity = activity as UpdatesActivity
        with(view) {
            preferencesAutoDeleteUpdates = requireViewById(R.id.preferences_auto_delete_updates)
            preferencesMeteredNetworkWarning = requireViewById(R.id.preferences_metered_network_warning)
            preferencesAutoUpdatesCheckInterval = requireViewById(R.id.preferences_auto_updates_check_interval)
            buttonLocalUpdate = requireViewById(R.id.button_local_update)
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        preferencesAutoUpdatesCheckInterval.setSelection(Utils.getUpdateCheckSetting(requireContext()))
        preferencesAutoDeleteUpdates.isChecked =
            prefs!!.getBoolean(Constants.PREF_AUTO_DELETE_UPDATES, false)
        preferencesMeteredNetworkWarning.isChecked =
            prefs!!.getBoolean(Constants.PREF_METERED_NETWORK_WARNING,
                prefs!!.getBoolean(Constants.PREF_MOBILE_DATA_WARNING, true))

        mUpdateImporter = UpdateImporter(requireActivity() as UpdatesActivity, this)

        buttonLocalUpdate.setOnClickListener {
            mUpdateImporter.openImportPicker()
        }
    }

    override fun onPause() {
        updatesActivity.onPause()
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        updatesActivity.onActivityResult(requestCode, resultCode, data)
    }

    override fun onImportStarted() {
        updatesActivity.onImportStarted()
    }

    override fun onImportCompleted(update: Update?) {
        updatesActivity.onImportCompleted(update)
    }

    override fun onDismiss(dialog: DialogInterface) {
        prefs!!.edit()
            .putInt(
                Constants.PREF_AUTO_UPDATES_CHECK_INTERVAL,
                preferencesAutoUpdatesCheckInterval.selectedItemPosition
            )
            .putBoolean(Constants.PREF_AUTO_DELETE_UPDATES, preferencesAutoDeleteUpdates.isChecked)
            .putBoolean(Constants.PREF_METERED_NETWORK_WARNING, preferencesMeteredNetworkWarning.isChecked)
            .apply()

        if (Utils.isUpdateCheckEnabled(requireContext())) {
            UpdatesCheckReceiver.scheduleRepeatingUpdatesCheck(requireContext())
        } else {
            UpdatesCheckReceiver.cancelRepeatingUpdatesCheck(requireContext())
            UpdatesCheckReceiver.cancelUpdatesCheck(requireContext())
        }

        super.onDismiss(dialog)
    }

    fun setupPreferenceSheet(updaterService: UpdaterService, updateView: UpdateView): PreferenceSheet {
        this.mUpdaterService = updaterService
        this.updateView = updateView
        return this
    }

    fun setUpdateImporter(updateImporter: UpdateImporter): PreferenceSheet {
        this.mUpdateImporter = updateImporter
        return this
    }

    fun addItem(downloadId: String) {
        if (mDownloadIds == null) {
            mDownloadIds = ArrayList()
        }
        mDownloadIds!!.add(0, downloadId)
        updateView.addItem(downloadId)
    }
}

