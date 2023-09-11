package com.dscvit.vitty.ui.schedule

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat.setNestedScrollingEnabled
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dscvit.vitty.R
import com.dscvit.vitty.activity.InstructionsActivity
import com.dscvit.vitty.adapter.PeriodAdapter
import com.dscvit.vitty.databinding.FragmentDayBinding
import com.dscvit.vitty.model.PeriodDetails
import com.dscvit.vitty.network.api.community.responses.timetable.Course
import com.dscvit.vitty.util.Constants
import com.dscvit.vitty.util.Constants.DEFAULT_QUOTE
import com.dscvit.vitty.util.Constants.USER_INFO
import com.dscvit.vitty.util.Quote
import com.dscvit.vitty.util.UtilFunctions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
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
    private val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
    lateinit var day: String
    private lateinit var scheduleViewModel: ScheduleViewModel


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
        scheduleViewModel = ViewModelProvider(this)[ScheduleViewModel::class.java]
        //get token and username from shared preferences
        val sharedPreferences = activity?.getSharedPreferences(USER_INFO, Context.MODE_PRIVATE)
        val token = sharedPreferences?.getString(Constants.COMMUNITY_TOKEN, "") ?: ""
        val username = requireArguments().getString("username") ?: sharedPreferences?.getString(Constants.COMMUNITY_USERNAME, null) ?: ""
        Timber.d("token $token username $username")
        Timber.d("pref username is ${sharedPreferences?.getString(Constants.COMMUNITY_USERNAME, null)}")
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

        scheduleViewModel.user.observe(viewLifecycleOwner){
            if(it!=null){
                Timber.d(it.toString())
                val timetableDays = it.timetable?.data
                when(day){
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
                        val saturday = null
                        setUpDayTimeTable(saturday)
                    }
                    "sunday" -> {
                        val sunday = null
                        setUpDayTimeTable(sunday)
                    }

                }

            }
        }
        /*if (uid != null) {
            db.collection("users")
                .document(uid)
                .collection("timetable")
                .document(day)
                .collection("periods")
                .get(Source.CACHE)
                .addOnSuccessListener { result ->
                    for (document in result) {
                        try {
                            val pd = PeriodDetails(
                                document.getString("courseCode")!!,
                                document.getString("courseName")!!,
                                document.getTimestamp("startTime")!!,
                                document.getTimestamp("endTime")!!,
                                document.getString("slot")!!,
                                document.getString("location")!!
                            )
                            courseList.add(pd)
                        } catch (e: Exception) {
                        }
                    }
                    scheduleSetup()
                }
                .addOnFailureListener { e ->
                    Timber.d("Auth error: $e")
                }
        }*/
    }

    private fun setUpDayTimeTable(day: List<Course>?) {
        courseList.clear()
        if(!day.isNullOrEmpty()){
            for (course in day){
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
        }
        scheduleSetup()

    }


    private fun parseTimeToTimestamp(timeString: String): Timestamp {
        try{
            val time = replaceYearIfZero(timeString)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            // Set the time zone of the date format to UTC
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = dateFormat.parse(time)
            Timber.d("Date----: $date")

            if (date != null) {
                val localTimeZone = TimeZone.getDefault()
                val localDate = Date(date.time + localTimeZone.rawOffset)
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
    private fun scheduleSetup() {
        binding.apply {
            if (courseList.isNotEmpty()) {
                dayList.scheduleLayoutAnimation()
                dayList.adapter = PeriodAdapter(courseList, fragID)
                dayList.layoutManager = LinearLayoutManager(context)
                noPeriod.visibility = View.INVISIBLE
            } else {
                binding.quoteLine.text = try {
                    Quote.getLine(context)
                } catch (_: Exception) {
                    DEFAULT_QUOTE
                }
                noPeriod.visibility = View.VISIBLE
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
