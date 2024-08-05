package sp.inetvpn.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.Toast
import sp.xray.lite.ui.SettingsActivity
import sp.inetvpn.BuildConfig
import sp.inetvpn.MainApplication
import sp.inetvpn.R
import sp.inetvpn.data.GlobalData
import sp.inetvpn.databinding.ActivityUsageBinding
import sp.inetvpn.util.LogManager
import sp.inetvpn.util.UsageConnectionManager
import sp.openconnect.fragments.LogFragment
import sp.openconnect.fragments.VPNProfileList
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit


/**
 * by Mehrab
 */
class UsageActivity : Activity() {
    private lateinit var binding: ActivityUsageBinding

    private val usageConnectionManager = UsageConnectionManager()

    private var isDeviceH: Boolean = "huawei".equals(Build.MANUFACTURER, ignoreCase = true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsageBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // bindings
        setupBindingUsage()
        // error prone
        showUsageCuTitle()
        checkDeviceFun()

        when (GlobalData.defaultItemDialog) {
            1 -> {
                binding.settingUsage.openVpnC.isChecked = true
                binding.settingUsage.v2rayC.isChecked = false
            }

            0 -> {
                binding.settingUsage.openVpnC.isChecked = false
                binding.settingUsage.v2rayC.isChecked = true
            }
        }

//                startActivity(Intent(this, LogActivity::class.java))
//                startActivity(Intent(this, ContactActivity::class.java))
//                overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left)

        binding.settingAngMain.setOnClickListener {
            startActivity(Intent(this, MainSettingsV2ray::class.java))
            overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left)
        }

        binding.settingCiscMain.setOnClickListener {
            findViewById<FrameLayout>(R.id.content_frame).visibility = View.VISIBLE
            fragmentManager.beginTransaction()
                .replace(R.id.content_frame, VPNProfileList())
                .commit()
        }

        binding.ciscoLog.setOnClickListener {
            findViewById<FrameLayout>(R.id.content_frame).visibility = View.VISIBLE
            fragmentManager.beginTransaction()
                .replace(R.id.content_frame, LogFragment())
                .commit()
//            startActivity(Intent(this, app.openconnect.MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
//            finish()
//            overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left)
        }

        binding.headerLayout.llForward.setOnClickListener {
            this.onBackPressed()
        }
        binding.linearLayoutBattery.setOnClickListener {
            checkLinearLayoutBattery()
        }
        binding.linearLayoutAboutme.setOnClickListener {
            openAboutMeActivity()
        }

        binding.switchUsageFastMode.setOnCheckedChangeListener { _, isChecked ->
            GlobalData.settingsStorage.putBoolean("cancel_fast", isChecked)
            GlobalData.cancelFast = isChecked
        }

        if (GlobalData.cancelFast) {
            binding.switchUsageFastMode.setChecked(true)
        } else {
            binding.switchUsageFastMode.setChecked(false)
        }

        // on below line we are adding check
        // change listener for our radio group.
        binding.settingUsage.radioPortocolGroup.setOnCheckedChangeListener { _, checkedId ->
            // on below line we are getting radio button from our group.
            val radioButton = findViewById<RadioButton>(checkedId)

            if (radioButton.text == "OpenVpn") {
                GlobalData.settingsStorage.putInt("default_connection_type", 1)
                GlobalData.defaultItemDialog = 1
            } else if (radioButton.text == "V2ray") {
                GlobalData.settingsStorage.putInt("default_connection_type", 0)
                GlobalData.defaultItemDialog = 0
            }
            // on below line we are displaying a toast message.
            Toast.makeText(
                this@UsageActivity,
                "تنظیم شد : " + radioButton.text,
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.settingUsage.openSettingV2.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun showUsageCuTitle() {
        var tvUsageCuTitle = GlobalData.NA
        try {
            val deviceCreated = GlobalData.settingsStorage.getString("device_created", "null")
            if (deviceCreated != "null") {
                val deviceTime = deviceCreated!!.toLong()
                val nowTime = System.currentTimeMillis()
                val elapsedTime = nowTime - deviceTime
                if (nowTime > deviceTime) {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
                    val hours = TimeUnit.MILLISECONDS.toHours(elapsedTime)
                    val days = TimeUnit.MILLISECONDS.toDays(elapsedTime)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime)
                    val timeString: String = if (elapsedTime in 120000..3599999) {
                        convertToFarsiNumber(minutes) + ' ' + GlobalData.minute_ago
                    } else if (elapsedTime in 3600000..7199999) {
                        convertToFarsiNumber(hours) + ' ' + GlobalData.hour_ago
                    } else if (elapsedTime in 7200000..86399999) {
                        convertToFarsiNumber(hours) + ' ' + GlobalData.hours_ago
                    } else if (elapsedTime in 86400000..172799999) {
                        convertToFarsiNumber(days) + ' ' + GlobalData.day_ago
                    } else if (elapsedTime >= 172800000) {
                        convertToFarsiNumber(days) + ' ' + GlobalData.days_ago
                    } else if (elapsedTime >= 60000) {
                        convertToFarsiNumber(minutes) + ' ' + GlobalData.minutes_ago
                    } else {
                        convertToFarsiNumber(seconds) + ' ' + GlobalData.seconds_ago
                    }
                    tvUsageCuTitle = GlobalData.device_time_txt + ' ' + timeString
                }
            }
        } catch (e: Exception) {
            val params = Bundle()
            params.putString("device_id", MainApplication.device_id)
            params.putString("exception", "UA1$e")
            LogManager.logEvent(params)
        } finally {
            binding.tvUsageCuTitle.text = tvUsageCuTitle
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupBindingUsage() {
        binding.tvUsageCuVersion.text = "${GlobalData.Version_txt} ${BuildConfig.VERSION_NAME}"

        // connections
        binding.connectionsUsage.tvUsageConnectionTodaySize.text =
            usageConnectionManager.getConnectionCountForToday().toString()

        binding.connectionsUsage.tvUsageConnectionYesterdaySize.text =
            usageConnectionManager.getConnectionCountForYesterday().toString()

        binding.connectionsUsage.tvUsageConnectionTotalSize.text =
            usageConnectionManager.getTotalConnections().toString()
    }

    private fun openAboutMeActivity() {
        startActivity(Intent(this, AboutMeActivity::class.java))
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left)
    }

    private fun checkLinearLayoutBattery() {
        try {
            if (isDeviceH) {
                val builder = AlertDialog.Builder(this@UsageActivity)
                builder.setTitle(GlobalData.default_usage_permissions_txt)
                    .setMessage(GlobalData.default_usage_permissions_backg_txt)
                    .setPositiveButton("Allow") { _, _ ->
                        try {
                            val intent = Intent()
                            intent.setComponent(
                                ComponentName(
                                    "com.huawei.systemmanager",
                                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                                )
                            )
                            startActivity(intent)
                        } catch (e: Exception) {
                            val params = Bundle()
                            params.putString("device_id", MainApplication.device_id)
                            params.putString("exception", "UA15$e")
                            LogManager.logEvent(params)
                        }
                    }.create().show()
                val params = Bundle()
                params.putString("device_id", MainApplication.device_id)
                params.putString("click", "amazon")
                params.putString("exception", "app_param_click")
                LogManager.logEvent(params)
            }
        } catch (e: Exception) {
            val params = Bundle()
            params.putString("device_id", MainApplication.device_id)
            params.putString("exception", "UA16$e")
            LogManager.logEvent(params)
        }
    }

    private fun checkDeviceFun() {
        try {
            if (isDeviceH) {
                binding.linearLayoutBattery.visibility = View.VISIBLE
                binding.linearLineLayoutBattery.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            val params = Bundle()
            params.putString("device_id", MainApplication.device_id)
            params.putString("exception", "UA14$e")
            LogManager.logEvent(params)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left)
    }

    companion object {
        fun convertToFarsiNumber(number: Long): String {
            val numberFormat = NumberFormat.getNumberInstance(Locale("fa"))
            return numberFormat.format(number)
        }
    }
}
