package com.dscvit.vitty.ui.schedule

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dscvit.vitty.R
import com.dscvit.vitty.adapter.PeriodAdapter
import com.dscvit.vitty.databinding.FragmentDayBinding
import com.dscvit.vitty.model.PeriodDetails
import com.dscvit.vitty.network.api.community.responses.timetable.Course
import com.dscvit.vitty.network.api.community.responses.user.UserResponse
import com.dscvit.vitty.util.Constants
import com.dscvit.vitty.util.Constants.DEFAULT_QUOTE
import com.dscvit.vitty.util.Constants.USER_INFO
import com.dscvit.vitty.util.Quote
import com.dscvit.vitty.util.UtilFunctions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class DayFragment : Fragment() {

    private lateinit var binding: FragmentDayBinding
    private val courseList: ArrayList<PeriodDetails> = ArrayList()
    private var fragID = -1
    private lateinit var sharedPref: SharedPreferences
    private val db = FirebaseFirestore.getInstance()
    private val days =
        listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
    lateinit var day: String
    private lateinit var scheduleViewModel: ScheduleViewModel
    private var isFriendsTimetable = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_day,
            container,
            false
        )
        binding.loadingView.visibility = View.VISIBLE
        scheduleViewModel = ViewModelProvider(this)[ScheduleViewModel::class.java]
        //get token and username from shared preferences
        val sharedPreferences = activity?.getSharedPreferences(USER_INFO, Context.MODE_PRIVATE)
        val token = sharedPreferences?.getString(Constants.COMMUNITY_TOKEN, "") ?: ""
        val username = requireArguments().getString("username") ?: sharedPreferences?.getString(
            Constants.COMMUNITY_USERNAME,
            null
        ) ?: ""
        isFriendsTimetable = requireArguments().getBoolean("isFriendsTimetable")
        Timber.d("token $token username $username")
        Timber.d(
            "pref username is ${
                sharedPreferences?.getString(
                    Constants.COMMUNITY_USERNAME,
                    null
                )
            }"
        )
        scheduleViewModel.getUserWithTimeTable(token, username)
        fragID = requireArguments().getString("frag_id")?.toInt()!!
        getData()

        binding.dayList.isNestedScrollingEnabled = false
        return binding.root
    }

    private fun getData() {
        sharedPref = activity?.getSharedPreferences(USER_INFO, Context.MODE_PRIVATE)!!
        courseList.clear()
        //val uid = sharedPref.getString("uid", "")
        day = if (days[fragID] == "saturday") sharedPref.getString(
            UtilFunctions.getSatModeCode(),
            "saturday"
        ).toString() else days[fragID]

        val cachedData = sharedPref.getString(Constants.CACHE_COMMUNITY_TIMETABLE, null)
        if (cachedData != null && !isFriendsTimetable) {
            // If cached data is available, load from cache
            Timber.d("Loading from cache")
            Timber.d("$cachedData")
            val response = Gson().fromJson(cachedData, UserResponse::class.java)
//            Toast.makeText(context, "Loaded from cache", Toast.LENGTH_SHORT).show()
            processTimetableData(response)

        }

        UtilFunctions.reloadWidgets(requireContext())

        scheduleViewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                //cache response for widget
                if(!isFriendsTimetable) {
                    val response = Gson().toJson(it)
                    val editor = sharedPref.edit()
                    editor.putString(Constants.CACHE_COMMUNITY_TIMETABLE, response)
                    editor.apply()
                    val cachedData = sharedPref.getString(Constants.CACHE_COMMUNITY_TIMETABLE, null)
                    Timber.d("cached data is $cachedData")
                }

//                Toast.makeText(context, "Updated Timetable from internet.", Toast.LENGTH_SHORT).show()


                Timber.d(it.toString())

                processTimetableData(it)

            } else {
                if (cachedData == null) {
                    Toast.makeText(context, "Error fetching timetable", Toast.LENGTH_SHORT).show()

                }
                binding.loadingView.visibility = View.GONE
            }
        }

    }

    private fun processTimetableData(userResponse: UserResponse?) {
        val timetableDays = userResponse?.timetable?.data
        when (day) {
            "monday" -> {
                val monday = timetableDays?.Monday
                setUpDayTimeTable(monday)
            }

            "tuesday" -> {
                val tuesday = timetableDays?.Tuesday
                setUpDayTimeTable(tuesday)
            }

            "wednesday" -> {
                val wednesday = timetableDays?.Wednesday
                setUpDayTimeTable(wednesday)
            }

            "thursday" -> {
                val thursday = timetableDays?.Thursday
                setUpDayTimeTable(thursday)
            }

            "friday" -> {
                val friday = timetableDays?.Friday
                setUpDayTimeTable(friday)
            }

            "saturday" -> {
                val saturday = timetableDays?.Saturday
                setUpDayTimeTable(saturday)
            }

            "sunday" -> {
                val sunday = timetableDays?.Sunday
                setUpDayTimeTable(sunday)
            }

        }
    }

    private fun setUpDayTimeTable(day: List<Course>?) {
        courseList.clear()
        if (!day.isNullOrEmpty()) {
            for (course in day) {
                val pd = PeriodDetails(
                    course.code,
                    course.name,
                    parseTimeToTimestamp(course.start_time),
                    parseTimeToTimestamp(course.end_time),
                    course.slot,
                    course.venue
                )
                courseList.add(pd)
            }
            courseList.sortBy { it.startTime }

        }
        scheduleSetup()

    }


    private fun parseTimeToTimestamp(timeString: String): Timestamp {
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

    private fun scheduleSetup() {
        binding.apply {
            if (courseList.isNotEmpty()) {
                dayList.scheduleLayoutAnimation()
                dayList.adapter = PeriodAdapter(courseList, fragID)
                dayList.layoutManager = LinearLayoutManager(context)
                noPeriod.visibility = View.INVISIBLE
                binding.loadingView.visibility = View.GONE
            } else {
                binding.quoteLine.text = try {
                    Quote.getLine(context)
                } catch (_: Exception) {
                    DEFAULT_QUOTE
                }
                noPeriod.visibility = View.VISIBLE
                binding.loadingView.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (days[fragID] == "saturday" && day != sharedPref.getString(
                UtilFunctions.getSatModeCode(),
                "saturday"
            ).toString()
        ) {
            getData()
        }
    }
}
