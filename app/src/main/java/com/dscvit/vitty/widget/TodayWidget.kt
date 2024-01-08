package com.dscvit.vitty.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.lifecycle.ViewModelProvider
import com.dscvit.vitty.R
import com.dscvit.vitty.activity.AuthActivity
import com.dscvit.vitty.network.api.community.APICommunityRestClient
import com.dscvit.vitty.network.api.community.RetrofitSelfUserListener
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import com.dscvit.vitty.service.TodayWidgetService
import com.dscvit.vitty.ui.schedule.ScheduleViewModel
import com.dscvit.vitty.util.ArraySaverLoader.saveArray
import com.dscvit.vitty.util.Constants
import com.dscvit.vitty.util.Constants.PERIODS
import com.dscvit.vitty.util.Constants.TIME_SLOTS
import com.dscvit.vitty.util.Constants.TODAY_INTENT
import com.dscvit.vitty.util.UtilFunctions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Implementation of App Widget functionality.
 */
class TodayWidget : AppWidgetProvider() {


    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateTodayWidget(context, appWidgetManager, appWidgetId, null, null, null)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateTodayWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    courseList: ArrayList<String>?,
    timeList: ArrayList<String>?,
    roomList: ArrayList<String>?
) {
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.today_widget)
    val intent = Intent(context, AuthActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context,
        TODAY_INTENT,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.today_widget, pendingIntent)
    val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
    val calendar: Calendar = Calendar.getInstance()
    val d = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    if (courseList == null) {
        fetchTodayFirestore(context, days[d], appWidgetManager, appWidgetId)
    } else if (courseList.isNotEmpty() || courseList.isEmpty()) {
        saveArray(courseList, "courses_today", context)
        saveArray(timeList!!, "time_today", context)
        saveArray(roomList!!, "class_rooms", context)
        val serviceIntent = Intent(context, TodayWidgetService::class.java)
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        val bundle1 = Bundle()
        bundle1.putStringArrayList(
            PERIODS,
            courseList
        )
        val bundle2 = Bundle()
        bundle2.putStringArrayList(
            TIME_SLOTS,
            timeList
        )
        views.setRemoteAdapter(R.id.periods, serviceIntent)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.periods)
    }

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

fun fetchTodayFirestore(
    context: Context,
    day: String,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) =
    runBlocking {
        fetchTodayData(context, day, appWidgetManager, appWidgetId)
    }

suspend fun fetchTodayData(
    context: Context,
    oldDay: String,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) =
    coroutineScope {
        val db = FirebaseFirestore.getInstance()
        val sharedPref = context.getSharedPreferences("login_info", Context.MODE_PRIVATE)!!
        val day = if (oldDay == "saturday") sharedPref.getString(
            UtilFunctions.getSatModeCode(),
            "saturday"
        ).toString() else oldDay
        val uid = sharedPref.getString("uid", "")
        val courseList: ArrayList<String> = ArrayList()
        val timeList: ArrayList<String> = ArrayList()
        val roomList: ArrayList<String> = ArrayList()
        val sharedPreferences = context.getSharedPreferences(Constants.USER_INFO, Context.MODE_PRIVATE)
        val token = sharedPreferences?.getString(Constants.COMMUNITY_TOKEN, "") ?: ""
        val username =  sharedPreferences?.getString(Constants.COMMUNITY_USERNAME, null) ?: ""
        APICommunityRestClient.instance.getUserWithTimeTable(token, username,

            object : RetrofitSelfUserListener {
                override fun onSuccess(call: Call<UserResponse>?, response: UserResponse?) {
                    val user = response
                    if(user?.timetable?.data == null){
                        updateTodayWidget(context, appWidgetManager, appWidgetId, courseList, timeList, roomList)
                        return
                    }
                    var today = user.timetable?.data?.Monday

                    when(day){
                        "monday" -> {
                            today = user.timetable?.data?.Monday
                        }
                        "tuesday" -> {
                            today = user.timetable?.data?.Tuesday
                        }
                        "wednesday" -> {
                            today = user.timetable?.data?.Wednesday
                        }
                        "thursday" -> {
                            today = user.timetable?.data?.Thursday
                        }
                        "friday" -> {
                            today = user.timetable?.data?.Friday
                        }
                        "saturday" -> {
                            today = user.timetable?.data?.Saturday
                        }
                        "sunday" -> {
                            today = user.timetable?.data?.Sunday
                        }
                    }
                    today = today?.sortedBy { it.start_time }
                    for(period in today!!){
                        var startTime = parseTimeToTimestamp(period.start_time).toDate()
                        var endTime = parseTimeToTimestamp(period.end_time).toDate()

                        val simpleDateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                        val sTime: String = simpleDateFormat.format(startTime).uppercase(Locale.ROOT)
                        val eTime: String = simpleDateFormat.format(endTime).uppercase(Locale.ROOT)
                        courseList.add(period.name)
                        timeList.add("$sTime - $eTime")
                        roomList.add(period.venue)
                    }

                   updateTodayWidget(context, appWidgetManager, appWidgetId, courseList, timeList, roomList)


                }

                override fun onError(call: Call<UserResponse>?, t: Throwable?) {
                    Timber.d("Error YO: $t")
                    updateTodayWidget(context, appWidgetManager, appWidgetId, courseList, timeList, roomList)

                }
            })
//        if (uid != null && uid != "") {
//            db.collection("users")
//                .document(uid)
//                .collection("timetable")
//                .document(day)
//                .collection("periods")
//                .get(Source.CACHE)
//                .addOnSuccessListener { result ->
//                    for (document in result) {
//                        try {
//                            val startTime: Date = document.getTimestamp("startTime")!!.toDate()
//                            val simpleDateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
//                            val sTime: String =
//                                simpleDateFormat.format(startTime).uppercase()
//
//                            val endTime: Date = document.getTimestamp("endTime")!!.toDate()
//                            val eTime: String =
//                                simpleDateFormat.format(endTime).uppercase()
//
//                            courseList.add(document.getString("courseName")!!)
//                            timeList.add("$sTime - $eTime")
//                            roomList.add(document.getString("location")!!)
//                        } catch (e: Exception) {
//                            Timber.d("Error: $e")
//                        }
//                    }
//                    updateTodayWidget(context, appWidgetManager, appWidgetId, courseList, timeList, roomList)
//                }
//                .addOnFailureListener { e ->
//                    Timber.d("Error YO: $e")
//                }
//        } else {
//            updateTodayWidget(context, appWidgetManager, appWidgetId, courseList, timeList, roomList)
//        }
    }

fun parseTimeToTimestamp(timeString: String): Timestamp {
    try{
        val time = replaceYearIfZero(timeString)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        // Set the time zone of the date format to UTC
        val date = dateFormat.parse(time)
        Timber.d("Date----: $date")

        if (date != null) {
            val localTimeZone = TimeZone.getDefault()
            val localDate = Date(date.time)
            return Timestamp(localDate)
        }else{
            return Timestamp.now()
        }
    }catch (e: Exception) {
        Timber.d("Date----: ${e.message}")
        return Timestamp.now()
    }
}

private fun replaceYearIfZero(dateStr: String): String {
    if (dateStr.startsWith("0")) {
        // Replace the first 4 characters with "2023"
        return "2023" + dateStr.substring(4)
    } else {
        // No change needed
        return dateStr
    }
}
