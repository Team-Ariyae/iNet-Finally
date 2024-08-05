package sp.inetvpn.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.navigation.NavigationView
import com.tencent.mmkv.MMKV
import sp.xray.lite.AppConfig
import sp.xray.lite.service.V2RayServiceManager
import sp.xray.lite.util.MmkvManager
import sp.xray.lite.util.Utils
import sp.xray.lite.viewmodel.MainViewModel
import de.blinkt.openvpn.OpenVpnApi
import de.blinkt.openvpn.core.App
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.OpenVPNThread
import de.blinkt.openvpn.core.VpnStatus
import sp.inetvpn.R
import sp.inetvpn.data.GlobalData
import sp.inetvpn.data.GlobalData.appValStorage
import sp.inetvpn.databinding.ActivityMainBinding
import sp.inetvpn.state.MainActivity.vpnState
import sp.inetvpn.util.CheckInternetConnection
import sp.inetvpn.util.UsageConnectionManager
import sp.openconnect.core.OpenConnectManagementThread
import sp.openconnect.core.OpenVpnService

/**
 * MehrabSp
 *
 * Github --> Team-Ariyae
 */
class MainActivity : sp.vpn.module.VpnActivity("domain:ir", true), NavigationView.OnNavigationItemSelectedListener {

    lateinit var binding: ActivityMainBinding

    private var setup: sp.inetvpn.setup.MainActivity? = null
    private var state: sp.inetvpn.state.MainActivity? = null

    private val usageConnectionManager = UsageConnectionManager()

    // cisco
    private var mConnectionState = OpenConnectManagementThread.STATE_DISCONNECTED

    /**
     * v2ray storage
     */
    // MMKV
    private val mainStorage by lazy {
        MMKV.mmkvWithID(
            MmkvManager.ID_MAIN,
            MMKV.MULTI_PROCESS_MODE
        )
    }
    private val settingsStorage by lazy {
        MMKV.mmkvWithID(
            MmkvManager.ID_SETTING,
            MMKV.MULTI_PROCESS_MODE
        )
    }

    /**
     * v2ray register
     */
    private val requestVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                startV2Ray()
            }
        }

    /**
     * enable connection button
     */
    private var enableButtonC: Boolean = true
    // ViewModel (V2ray)
    private val mainViewModel: MainViewModel by viewModels()
    override fun stateV2rayVpn(isRunning: Boolean) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setup = sp.inetvpn.setup.MainActivity(this, binding, mainViewModel)
        state = sp.inetvpn.state.MainActivity(this, binding, setup)

        state?.handlerSetupFirst() // set default state
        setup?.setupAll()
    }

    override fun setTestStateLayout(content: String) {

    }

    fun handleButtonConnect() {
        if (enableButtonC) {
            enableButtonC = false
            if (vpnState != 1) {
                when (GlobalData.defaultItemDialog) {
                    2 -> ConnectToCisco()
                    1 -> connectToOpenVpn()
                    0 -> connectToV2ray()
                }
            } else {
                when (GlobalData.defaultItemDialog) {
                    2 -> StopCisco()
                    1 -> stopVpn()
                    0 -> connectToV2ray()
                }
            }
            enableButtonC = true
        } else showToast("لطفا کمی صبر کنید..")
    }

    /**
     * this is Cisco
     */

    fun ConnectToCisco(){
        if (!GlobalData.isStart) {

            try {
                usageConnectionManager.establishConnection()

                try {
                    val file = GlobalData.connectionStorage.getString("fileC", null)

                    if (file != null) {
                        setup?.setNewImage()
                        state?.setNewVpnState(1)

                        val res: Boolean = CiscoCreateProfileWithHostName(file)

                        if(!res){
                            Toast.makeText(this, "مشکلی در ساخت پروفایل پیش امد!", Toast.LENGTH_SHORT).show()
                            StopCisco()
                        }else{
                            Toast.makeText(this, "در حال اتصال ...", Toast.LENGTH_SHORT).show()

                            CiscoStartVPNWithProfile()
                        }

                    } else {
                        startServersActivity()
                        Toast.makeText(this, "ابتدا یک سرور را انتخاب کنید", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }

            } catch (e: Exception) {
                Log.d("CISCO", "BUG: $e")
                showToast("وصل نشد!")
                StopCisco()
            }
        }else{
            StopCisco()
        }
    }

    fun StopCisco(){
        try{
            showToast(" قطع شد !")
            CiscoStopVPN()
            GlobalData.isStart = false
        }catch (e: Exception){
            showToast("مشکلی در قطع اتصال سیسکو پیش امد!")
        }finally {
            state?.setNewVpnState(0)
        }
    }

    /*
     */

    private fun connectToV2ray() {
        if (mainViewModel.isRunning.value == true) {
            Utils.stopVService(this)
            state?.setNewVpnState(0)
        } else if ((settingsStorage?.decodeString(AppConfig.PREF_MODE) ?: "VPN") == "VPN") {
            val intent = VpnService.prepare(this)
            if (intent == null) {
                startV2Ray()
            } else {
                requestVpnPermission.launch(intent)
            }
        } else {
            startV2Ray()
        }
    }

    private fun connectToOpenVpn() {
        if (GlobalData.isStart) {
            confirmDisconnect()
        } else {
            prepareVpn()
        }
    }

    /**
     * Stop vpn
     *
     * @return boolean: VPN status
     */
    private fun stopVpn(): Boolean {
        try {
            OpenVPNThread.stop()
            state?.setNewVpnState(0)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Show show disconnect confirm dialog
     */
    private fun confirmDisconnect() {
        if (GlobalData.cancelFast) {
            stopVpn()
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("ایا میخواهید اتصال را قطع کنید ؟")
            builder.setPositiveButton(
                "قطع اتصال"
            ) { _, _ -> stopVpn() }

            builder.setNegativeButton(
                "لغو"
            ) { _, _ ->
                // User cancelled the dialog
            }

            // Create the AlertDialog
            val dialog = builder.create()
            dialog.show()
        }
    }

    /**
     * Prepare for vpn connect with required permission
     */
    private fun prepareVpn() {
        if (!GlobalData.isStart) {
            if (CheckInternetConnection.netCheck(this)) {
                // Checking permission for network monitor
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    startActivityForResult(intent, 1)
                } else startVpn() //have already permission

            } else {

                // No internet connection available
                showToast("شما به اینترنت متصل نیستید !!")
                state?.handleErrorWhenConnect()
            }
        } else if (stopVpn()) {

            // VPN is stopped, show a Toast message.
            showToast("با موفقیت قطع شد")
        }
    }

    /**
     * Taking permission for network access
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            33 -> {
                if (resultCode == Activity.RESULT_OK) {
                    // اطلاعاتی که از اکتیویتی دوم دریافت می‌کنید
                    val result = data?.getBooleanExtra("restart", false)
                    if (result == true) {
                        restartOpenVpnServer()
                    }
                    // انجام کار خاص با استفاده از callback
                }
            }

            else -> {
                if (resultCode == RESULT_OK) {

                    //Permission granted, start the VPN
                    startVpn()
                } else {
                    showToast("دسترسی رد شد !! ")
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun CiscoUpdateUI(service: OpenVpnService?) {
        val newState = service!!.connectionState

        service.startActiveDialog(this)

        Log.d("OPENCONNECT S", newState.toString())

        if (mConnectionState !== newState) {
            if (newState == OpenConnectManagementThread.STATE_DISCONNECTED) {
                // stop
                GlobalData.isStart = false
                state?.setNewVpnState(0)
            } else if (mConnectionState == OpenConnectManagementThread.STATE_DISCONNECTED) {
                // start
                GlobalData.isStart = true
                state?.setNewVpnState(2)
            }
            mConnectionState = newState
        }
    }

    /**
     * Start the VPN
     */
    private fun startVpn() {
        usageConnectionManager.establishConnection()

        try {
            val file = GlobalData.connectionStorage.getString("fileO", null)
            val uL = appValStorage.getString("usernameLogin", null)
            val uU = appValStorage.getString("usernamePassword", null)

            if (file != null) {
                setup?.setNewImage()
                state?.setNewVpnState(1)

                App.clearDisallowedPackageApplication()
                App.addArrayDisallowedPackageApplication(GlobalData.disableAppsList)

                OpenVpnApi.startVpn(this, file, "Japan", uL, uU)

                // Update log
                Toast.makeText(this, "در حال اتصال ...", Toast.LENGTH_SHORT).show()

            } else {
                startServersActivity()
                Toast.makeText(this, "ابتدا یک سرور را انتخاب کنید", Toast.LENGTH_SHORT).show()
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    /**
     * Status change with corresponding vpn connection status
     *
     * @param connectionState
     */
    fun setStatus(connectionState: String?) {
        if (connectionState != null) {
            when (connectionState) {
                "DISCONNECTED" -> {
                    stopVpn()
//                    setDefaultStatus() TODO()
                }

                "CONNECTED" -> {
                    state?.setNewVpnState(2)
//                    checkInformationUser(this)
                }

                "WAIT" -> state?.setNewVpnState(1)
                "AUTH" -> state?.handleAUTH()
                "RECONNECTING" -> state?.setNewVpnState(1)
                "NONETWORK" -> state?.handleErrorWhenConnect()
            }
        }
    }

    /**
     * Receive broadcast message
     */
    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                setStatus(intent.getStringExtra("state"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                var duration = intent.getStringExtra("duration")
                var lastPacketReceive = intent.getStringExtra("lastPacketReceive")
                var byteIn = intent.getStringExtra("byteIn")
                var byteOut = intent.getStringExtra("byteOut")
                if (duration == null) duration = "00:00:00"
                if (lastPacketReceive == null) lastPacketReceive = "0"
                if (byteIn == null) byteIn = " "
                if (byteOut == null) byteOut = " "
                updateConnectionStatus(duration, lastPacketReceive, byteIn, byteOut)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun CiscoCurrentUserName(): String {
        var ul = appValStorage.getString("usernameLogin", null)
        if(ul == null) ul = ""
        return ul
    }

    override fun CiscoCurrentPassWord(): String {
        var ul = appValStorage.getString("usernamePassword", null)
        if(ul == null) ul = ""
        return ul
    }

    /**
     * Restart OpenVpn
     */
    private fun restartOpenVpnServer() {
        // Stop previous connection
        if (GlobalData.isStart) {
            stopVpn()
            // Delay for start
            Handler().postDelayed({ prepareVpn() }, 500)
        }
    }

    /**
     * v2ray
     */
    // v2ray
    private fun startV2Ray() {
        if (mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER).isNullOrEmpty()) {
            state?.setNewVpnState(0)
            return
        }
        // Set loader for V2ray
        state?.setNewVpnState(1)
        setup?.showCircle()
        // Start
        V2RayServiceManager.startV2Ray(this)
        usageConnectionManager.establishConnection()
        // Hide loader
        setup?.hideCircle()
        state?.setNewVpnState(2)
    }

    fun setStateFromOtherClass(newState: Int) {
        state?.setNewVpnState(newState)
    }

    fun setFooterFromOtherClass(newState: Int) {
        state?.setNewFooterState(newState)
    }

    // drawer options
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        setup?.navigationListener(item)
//        binding.drawerLayout.closeDrawer(GravityCompat.START) // bug
        return true
    }

    /**
     * Resume main activity, Set new icon server..
     */
    override fun onResume() {
        super.onResume()

        setup?.setNewImage()
        setup?.handleCountryImage()

        // Set broadcast for OpenVpn
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter("connectionState"))

        state?.restoreTodayTextTv()
    }

    override fun OpenVpnStatus(str: String?, err: Boolean?, errmsg: String?) {
    }

    override fun updateConnectionStatus(
        duration: String?,
        lastPacketReceive: String?,
        byteIn: String?,
        byteOut: String?
    ) {}

    fun startServersActivity() {
        val servers = Intent(this@MainActivity, ServersActivity::class.java)
        startActivityForResult(servers, 33)
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left)
    }

    fun startAngActivity() {
        startActivity(Intent(this@MainActivity, MainAngActivity::class.java))
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            moveTaskToBack(true)
            super.onBackPressed()
        }
    }

}
