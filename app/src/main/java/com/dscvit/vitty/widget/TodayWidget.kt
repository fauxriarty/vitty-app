package com.dscvit.vitty.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.dscvit.vitty.R
import com.dscvit.vitty.activity.AuthActivity
import com.dscvit.vitty.network.api.community.APICommunityRestClient
import com.dscvit.vitty.network.api.community.RetrofitSelfUserListener
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import com.dscvit.vitty.service.TodayWidgetService
import com.dscvit.vitty.util.ArraySaverLoader.saveArray
import com.dscvit.vitty.util.Constants
import com.dscvit.vitty.util.Constants.PERIODS
import com.dscvit.vitty.util.Constants.TIME_SLOTS
import com.dscvit.vitty.util.Constants.TODAY_INTENT
import com.dscvit.vitty.util.UtilFunctions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
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


    // Add the following lines to update the text with the current date and time
    val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
    val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    val currentDateTime = "$currentDate $currentTime"

    // Assuming "refesr_text" is the ID of the TextView you want to update
//    views.setTextViewText(R.id.refresh_widget, currentDateTime)


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
        val sharedPreferences =
            context.getSharedPreferences(Constants.USER_INFO, Context.MODE_PRIVATE)
        val token = sharedPreferences?.getString(Constants.COMMUNITY_TOKEN, "") ?: ""
        val username = sharedPreferences?.getString(Constants.COMMUNITY_USERNAME, null) ?: ""
        val cachedData = sharedPref.getString(Constants.CACHE_COMMUNITY_TIMETABLE, null)
        if (cachedData != null) {
            // If cached data is available, load from cache
            Timber.d("Loading from cache")
            Timber.d("$cachedData")
            val response = Gson().fromJson(cachedData, UserResponse::class.java)

            val user = response
            if (user?.timetable?.data == null) {
                updateTodayWidget(
                    context,
                    appWidgetManager,
                    appWidgetId,
                    courseList,
                    timeList,
                    roomList
                )
                return@coroutineScope
            }
            var today = user.timetable?.data?.Monday

            when (day) {
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
            Timber.d("Today: ${user.timetable}")
            today = today?.sortedBy { it.start_time }
            if (today != null) {
                for (period in today) {
                    var startTime = parseTimeToTimestamp(period.start_time).toDate()
                    var endTime = parseTimeToTimestamp(period.end_time).toDate()

                    val simpleDateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val sTime: String = simpleDateFormat.format(startTime).uppercase(Locale.ROOT)
                    val eTime: String = simpleDateFormat.format(endTime).uppercase(Locale.ROOT)
                    courseList.add(period.name)
                    timeList.add("$sTime - $eTime")
                    roomList.add(period.venue)
                }
            }

            updateTodayWidget(
                context,
                appWidgetManager,
                appWidgetId,
                courseList,
                timeList,
                roomList
            )
        }


        if (cachedData == null) {
            APICommunityRestClient.instance.getUserWithTimeTable(token, username,

                object : RetrofitSelfUserListener {
                    override fun onSuccess(call: Call<UserResponse>?, response: UserResponse?) {
                        val user = response

                        //cache response for widget
                        val jsonResponse = Gson().toJson(response)
                        val editor = sharedPref.edit()
                        editor.putString(Constants.CACHE_COMMUNITY_TIMETABLE, jsonResponse)
                        editor.apply()

                        if (user?.timetable?.data == null) {
                            updateTodayWidget(
                                context,
                                appWidgetManager,
                                appWidgetId,
                                courseList,
                                timeList,
                                roomList
                            )
                            return
                        }
                        var today = user.timetable?.data?.Monday

                        when (day) {
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
                        val todayTimeTable = today
                        if(todayTimeTable != null) {

                            for (period in todayTimeTable) {
                                var startTime = parseTimeToTimestamp(period.start_time).toDate()
                                var endTime = parseTimeToTimestamp(period.end_time).toDate()

                                val simpleDateFormat =
                                    SimpleDateFormat("h:mm a", Locale.getDefault())
                                val sTime: String =
                                    simpleDateFormat.format(startTime).uppercase(Locale.ROOT)
                                val eTime: String =
                                    simpleDateFormat.format(endTime).uppercase(Locale.ROOT)
                                courseList.add(period.name)
                                timeList.add("$sTime - $eTime")
                                roomList.add(period.venue)
                            }
                        }

                        updateTodayWidget(
                            context,
                            appWidgetManager,
                            appWidgetId,
                            courseList,
                            timeList,
                            roomList
                        )


                    }

                    override fun onError(call: Call<UserResponse>?, t: Throwable?) {
                        Timber.d("Error YO: $t")
                        updateTodayWidget(
                            context,
                            appWidgetManager,
                            appWidgetId,
                            courseList,
                            timeList,
                            roomList
                        )

                    }
                })
        }


    }


fun parseTimeToTimestamp(timeString: String): Timestamp {
    try {
        val time = replaceYearIfZero(timeString)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        // Set the time zone of the date format to UTC
        val date = dateFormat.parse(time)
        Timber.d("Date----: $date")

        if (date != null) {
            val localTimeZone = TimeZone.getDefault()
            val localDate = Date(date.time)
            return Timestamp(localDate)
        } else {
            return Timestamp.now()
        }
    } catch (e: Exception) {
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
