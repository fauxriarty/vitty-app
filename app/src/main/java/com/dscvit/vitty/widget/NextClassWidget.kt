package com.dscvit.vitty.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.dscvit.vitty.R
import com.dscvit.vitty.activity.AuthActivity
import com.dscvit.vitty.activity.NavigationActivity
import com.dscvit.vitty.model.PeriodDetails
import com.dscvit.vitty.network.api.community.APICommunityRestClient
import com.dscvit.vitty.network.api.community.RetrofitSelfUserListener
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import com.dscvit.vitty.util.Constants
import com.dscvit.vitty.util.Constants.NEXT_CLASS_INTENT
import com.dscvit.vitty.util.Constants.NEXT_CLASS_NAV_INTENT
import com.dscvit.vitty.util.Quote
import com.dscvit.vitty.util.RemoteConfigUtils
import com.dscvit.vitty.util.UtilFunctions
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

/**
 * Implementation of App Widget functionality.
 */
class NextClassWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateNextClassWidget(context, appWidgetManager, appWidgetId, null)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateNextClassWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    pd: PeriodDetails?
) {
    val views = RemoteViews(context.packageName, R.layout.next_class_widget)
    val intent = Intent(context, AuthActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context,
        NEXT_CLASS_INTENT,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.next_class_widget, pendingIntent)


    // Add the following lines to update the text with the current date and time
    val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
    val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    val currentDateTime = "$currentDate $currentTime"

    // Assuming "refresh_text" is the ID of the TextView you want to update
//    views.setTextViewText(R.id.refresh_widget, currentDateTime)

    val sharedPref = context.getSharedPreferences("login_info", Context.MODE_PRIVATE)!!
    if (sharedPref.getBoolean(Constants.EXAM_MODE, false)) {
        views.setTextViewText(
            R.id.course_name,
            "No classes mode turned on"
        )
        views.setTextViewText(
            R.id.period_time,
            "Turn it off to get notifications"
        )
        views.setViewVisibility(
            R.id.class_nav_button,
            View.GONE
        )
        views.setViewVisibility(
            R.id.class_id,
            View.GONE
        )

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
        return
    }

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

    if (pd == null) {
        fetchFirestore(context, days[d], calendar, appWidgetManager, appWidgetId)
    } else {
        val startTime: Date = pd.startTime.toDate()
        val simpleDateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val sTime: String = simpleDateFormat.format(startTime).uppercase(Locale.ROOT)

        val endTime: Date = pd.endTime.toDate()
        val eTime: String = simpleDateFormat.format(endTime).uppercase(Locale.ROOT)

        if (pd.courseName != "") {
            views.setTextViewText(R.id.course_name, pd.courseName)
            views.setTextViewText(R.id.period_time, "$sTime - $eTime")

            if (!RemoteConfigUtils.getOnlineMode()) {
                views.setViewVisibility(
                    R.id.class_nav_button,
                    View.VISIBLE
                )
                val clickIntent = Intent(context, NavigationActivity::class.java)
                clickIntent.putExtra("classId", pd.roomNo)
                val mapPendingIntent = PendingIntent.getActivity(
                    context,
                    NEXT_CLASS_NAV_INTENT,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.class_nav_button, mapPendingIntent)
            } else {
                views.setViewVisibility(
                    R.id.class_nav_button,
                    View.GONE
                )
            }
        } else {
            views.setTextViewText(
                R.id.course_name,
                context.getString(R.string.no_more_classes_today)
            )
            views.setTextViewText(
                R.id.period_time,
                Quote.getLine(context)
            )
            views.setViewVisibility(
                R.id.class_nav_button,
                View.GONE
            )
        }
        views.setViewVisibility(
            R.id.class_id,
            View.VISIBLE
        )
        views.setTextViewText(R.id.class_id, pd.roomNo)
    }

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

fun fetchFirestore(
    context: Context,
    day: String,
    calendar: Calendar,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) =
    runBlocking {
        fetchData(context, day, calendar, appWidgetManager, appWidgetId)
    }

suspend fun fetchData(
    context: Context,
    oldDay: String,
    calendar: Calendar,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) =
    coroutineScope {
        val db = FirebaseFirestore.getInstance()
        val sharedPref = context.getSharedPreferences("login_info", Context.MODE_PRIVATE)!!
        val day = if (oldDay == "saturday") sharedPref.getString(
            UtilFunctions.getSatModeCode(),
            "saturday"
        )
            .toString() else oldDay
        val uid = sharedPref.getString("uid", "")
        var pd = PeriodDetails()
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
                pd.courseName = ""
                updateNextClassWidget(context, appWidgetManager, appWidgetId, pd)
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
            today = today?.sortedBy { it.start_time }
            if (today != null) {
                for (period in today) {

                    try {
                        val start = Calendar.getInstance()
                        val s = Calendar.getInstance()
                        s.time = parseTimeToTimestamp(period.start_time).toDate()
                        start[Calendar.HOUR_OF_DAY] = s[Calendar.HOUR_OF_DAY]
                        start[Calendar.MINUTE] = s[Calendar.MINUTE]
                        Timber.d("$calendar")
                        Timber.d("$start")
                        if (start.time > calendar.time) {
                            val end = Calendar.getInstance()
                            val e = Calendar.getInstance()
                            e.time = parseTimeToTimestamp(period.end_time).toDate()
                            end[Calendar.HOUR_OF_DAY] = e[Calendar.HOUR_OF_DAY]
                            end[Calendar.MINUTE] = e[Calendar.MINUTE]
                            Timber.d("$end")
                            if (end.time > calendar.time) {
                                pd = PeriodDetails(
                                    period.code,
                                    period.name,
                                    parseTimeToTimestamp(period.start_time),
                                    parseTimeToTimestamp(period.end_time),
                                    period.slot,
                                    period.venue
                                )
                                break
                            }
                        } else {
                            pd.courseName = ""
                            val simpleDateFormat =
                                SimpleDateFormat("h:mm a", Locale.getDefault())
                            val sTime: String =
                                simpleDateFormat.format(calendar.time).uppercase(Locale.ROOT)
                            Timber.d("LOL: $sTime")
                        }
                    } catch (e: Exception) {
                        pd.courseName = ""
                        Timber.d("F: $e")
                    }


                }
            }
            updateNextClassWidget(context, appWidgetManager, appWidgetId, pd)

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
                            pd.courseName = ""
                            updateNextClassWidget(context, appWidgetManager, appWidgetId, pd)
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
                                today = user?.timetable?.data?.Friday
                            }

                            "saturday" -> {
                                today = user?.timetable?.data?.Saturday
                            }

                            "sunday" -> {
                                today = user?.timetable?.data?.Sunday
                            }
                        }
                        today = today?.sortedBy { it.start_time }

                        val todayTimetable = today
                        if(todayTimetable != null){
                            for (period in todayTimetable) {
                                try {
                                    val start = Calendar.getInstance()
                                    val s = Calendar.getInstance()
                                    s.time = parseTimeToTimestamp(period.start_time).toDate()
                                    start[Calendar.HOUR_OF_DAY] = s[Calendar.HOUR_OF_DAY]
                                    start[Calendar.MINUTE] = s[Calendar.MINUTE]
                                    Timber.d("$calendar")
                                    Timber.d("$start")
                                    if (start.time > calendar.time) {
                                        val end = Calendar.getInstance()
                                        val e = Calendar.getInstance()
                                        e.time = parseTimeToTimestamp(period.end_time).toDate()
                                        end[Calendar.HOUR_OF_DAY] = e[Calendar.HOUR_OF_DAY]
                                        end[Calendar.MINUTE] = e[Calendar.MINUTE]
                                        Timber.d("$end")
                                        if (end.time > calendar.time) {
                                            pd = PeriodDetails(
                                                period.code,
                                                period.name,
                                                parseTimeToTimestamp(period.start_time),
                                                parseTimeToTimestamp(period.end_time),
                                                period.slot,
                                                period.venue
                                            )
                                            break
                                        }
                                    } else {
                                        pd.courseName = ""
                                        val simpleDateFormat =
                                            SimpleDateFormat("h:mm a", Locale.getDefault())
                                        val sTime: String =
                                            simpleDateFormat.format(calendar.time)
                                                .uppercase(Locale.ROOT)
                                        Timber.d("LOL: $sTime")
                                    }
                                } catch (e: Exception) {
                                    pd.courseName = ""
                                    Timber.d("F: $e")
                                }


                            }
                        }
                        updateNextClassWidget(context, appWidgetManager, appWidgetId, pd)

                    }

                    override fun onError(call: Call<UserResponse>?, t: Throwable?) {
                        pd.courseName = ""
                        updateNextClassWidget(context, appWidgetManager, appWidgetId, pd)
                    }
                })
        }


    }
