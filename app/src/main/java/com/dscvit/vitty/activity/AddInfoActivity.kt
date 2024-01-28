package com.dscvit.vitty.activity

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.dscvit.vitty.BuildConfig
import com.dscvit.vitty.R
import com.dscvit.vitty.databinding.ActivityAddInfoBinding
import com.dscvit.vitty.receiver.AlarmReceiver
import com.dscvit.vitty.ui.auth.AuthViewModel
import com.dscvit.vitty.util.ArraySaverLoader
import com.dscvit.vitty.util.Constants
import com.dscvit.vitty.util.LogoutHelper
import com.dscvit.vitty.util.NotificationHelper
import com.dscvit.vitty.util.UtilFunctions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import timber.log.Timber
import java.util.Date

class AddInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddInfoBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var prefs: SharedPreferences
    private val db = FirebaseFirestore.getInstance()
    private val days =
        listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
    private var uid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_info)
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        prefs = getSharedPreferences(Constants.USER_INFO, Context.MODE_PRIVATE)
        uid = prefs.getString(Constants.UID, "").toString()
        setupToolbar()
        setGDSCVITChannel()

        binding.continueButton.setOnClickListener {
            setupContinueButton()
        }

        authViewModel.signInResponse.observe(this) {
            if (it != null) {
                prefs.edit().putString(Constants.COMMUNITY_TOKEN, it.token).apply()
                prefs.edit().putString(Constants.COMMUNITY_NAME, it.name).apply()
                prefs.edit().putString(Constants.COMMUNITY_PICTURE, it.picture).apply()
            } else {
                Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                binding.loadingView.visibility = View.GONE
            }
        }

        authViewModel.user.observe(this) {
            Timber.d("User: ${it}")
            if (it != null) {

                val timetableDays = it.timetable?.data
                if (!timetableDays?.Monday.isNullOrEmpty() || !timetableDays?.Tuesday.isNullOrEmpty() || !timetableDays?.Wednesday.isNullOrEmpty() || !timetableDays?.Thursday.isNullOrEmpty() || !timetableDays?.Friday.isNullOrEmpty()
                    || !timetableDays?.Saturday.isNullOrEmpty() || !timetableDays?.Sunday.isNullOrEmpty()
                ) {
                    binding.loadingView.visibility = View.GONE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        createNotificationChannels()
                    } else {
                        tellUpdated()
                    }
                    prefs.edit().putBoolean(Constants.COMMUNITY_TIMETABLE_AVAILABLE, true).apply()
                } else {
                    val intent = Intent(this, InstructionsActivity::class.java)
                    binding.loadingView.visibility = View.GONE
                    startActivity(intent)
                    finish()
                }
            }
        }


        binding.etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val username = s.toString().trim()
                authViewModel.checkUsername(username)
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        authViewModel.usernameValidity.observe(this) {
            Timber.d("Validity: ${it}")
            if (it != null) {
                binding.usernameValidity.visibility = View.VISIBLE
                binding.usernameValidity.text = it.detail
                if (it.detail == "Username is valid") {
                    binding.usernameValidity.setTextColor(getColor(R.color.white))
                } else {
                    binding.usernameValidity.setTextColor(getColor(R.color.red))
                }
            }
        }

    }


    private fun setupContinueButton() {
        binding.loadingView.visibility = View.VISIBLE
        val uuid = prefs.getString(Constants.UID, null)
        val username = binding.etUsername.text.toString().trim { it <= ' ' }
        val regno = binding.etRegno.text.toString().uppercase().trim { it <= ' ' }
        val regexPattern = Regex("^[0-9]{2}[a-zA-Z]{3}[0-9]{4}$")
        if (username.isEmpty() || regno.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_LONG).show()
            binding.loadingView.visibility = View.GONE
        } else if (!regexPattern.matches(regno)) {
            Toast.makeText(this, getString(R.string.invalid_regno), Toast.LENGTH_LONG).show()
            binding.loadingView.visibility = View.GONE
        } else {
            if (uuid != null) {
                prefs.edit().putString(Constants.COMMUNITY_USERNAME, username).apply()
                prefs.edit().putString(Constants.COMMUNITY_REGNO, regno).apply()
                authViewModel.signInAndGetTimeTable(username, regno, uuid)
            } else {
                Toast.makeText(this, getString(R.string.something_went_wrong), Toast.LENGTH_LONG)
                    .show()
                binding.loadingView.visibility = View.GONE
            }
        }


    }

    private fun setupToolbar() {
        binding.addInfoToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.close -> {
                    LogoutHelper.logout(this, this as Activity, prefs)
                    true
                }

                else -> false
            }
        }
    }

    private fun createNotificationChannels() {
        binding.loadingView.visibility = View.VISIBLE
        setNotificationGroup()
        val notifChannels = ArraySaverLoader.loadArray(Constants.NOTIFICATION_CHANNELS, this)
        for (notifChannel in notifChannels) {
            if (notifChannel != null) {
                NotificationHelper.deleteNotificationChannel(this, notifChannel.toString())
            }
        }
        val newNotifChannels: ArrayList<String> = ArrayList()
        for (day in days) {
            db.collection("users")
                .document(uid)
                .collection("timetable")
                .document(day)
                .collection("periods")
                .get(Source.SERVER)
                .addOnSuccessListener { result ->
                    for (document in result) {
                        var cn = document.getString("courseName")
                        cn = if (cn.isNullOrEmpty()) "Default" else cn
                        val cc = document.getString("courseCode")
                        NotificationHelper.createNotificationChannel(
                            this,
                            cn,
                            "Course Code: $cc",
                            Constants.GROUP_ID
                        )
                        newNotifChannels.add(cn)
                        Timber.d(cn)
                    }
                    ArraySaverLoader.saveArray(
                        newNotifChannels,
                        Constants.NOTIFICATION_CHANNELS,
                        this
                    )

                    if (day == "sunday")
                        tellUpdated()
                }
                .addOnFailureListener { e ->
                    Timber.d("Error: $e")
                }
        }
    }

    private fun tellUpdated() {
        prefs.edit().putInt(Constants.TIMETABLE_AVAILABLE, 1).apply()
        prefs.edit().putInt(Constants.UPDATE, 0).apply()
        val updated = hashMapOf(
            "isTimetableAvailable" to true,
            "isUpdated" to false
        )
        db.collection("users")
            .document(uid)
            .set(updated)
            .addOnSuccessListener {
                setAlarm()
                UtilFunctions.reloadWidgets(this)
                val pm: PowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    Toast.makeText(
                        this,
                        "Please turn off the Battery Optimization Settings for VITTY to receive notifications on time.",
                        Toast.LENGTH_LONG
                    ).show()
                    val pmIntent = Intent()
                    pmIntent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    startActivity(pmIntent)
                } else {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Timber.d("Error: $e")
            }
    }


    private fun setGDSCVITChannel() {
        if (!prefs.getBoolean("gdscvitChannelCreated", false)) {
            NotificationHelper.createNotificationGroup(
                this,
                getString(R.string.gdscvit),
                Constants.GROUP_ID_2
            )
            NotificationHelper.createNotificationChannel(
                this,
                getString(R.string.default_notification_channel_name),
                "Notifications from GDSC VIT",
                Constants.GROUP_ID_2
            )
            prefs.edit {
                putBoolean("gdscvitChannelCreated", true)
                apply()
            }
        }
    }

    private fun setNotificationGroup() {
        if (!prefs.getBoolean("groupCreated", false)) {
            NotificationHelper.createNotificationGroup(
                this,
                getString(R.string.notif_group),
                Constants.GROUP_ID
            )
            prefs.edit {
                putBoolean("groupCreated", true)
                apply()
            }
        }
    }

    private fun setAlarm() {
        if (!prefs.getBoolean(Constants.EXAM_MODE, false)) {
            if (prefs.getInt(Constants.VERSION_CODE, 0) != BuildConfig.VERSION_CODE) {
                val intent = Intent(this, AlarmReceiver::class.java)

                val pendingIntent =
                    PendingIntent.getBroadcast(
                        this, Constants.ALARM_INTENT, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

                val date = Date().time

                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    date,
                    (1000 * 60 * Constants.NOTIF_DELAY).toLong(),
                    pendingIntent
                )

                prefs.edit {
                    putInt(Constants.VERSION_CODE, BuildConfig.VERSION_CODE)
                    apply()
                }
            }
        }
    }

}


